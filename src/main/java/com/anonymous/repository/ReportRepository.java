package com.anonymous.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// Read-only aggregate queries for the report screens. Dates are stored in UTC; Vietnam has no daylight
// saving, so "+7 hours" gives the local business day without needing MySQL time zone tables.
// Only COMPLETED invoices / returns count as sales.
@Repository
public class ReportRepository {

    private static final String LOCAL_DAY = "DATE_FORMAT(DATE_ADD(%s, INTERVAL 7 HOUR), '%%Y-%%m-%%d')";

    private static final String COMPLETED_INVOICES =
            " FROM invoice i JOIN status_invoice s ON s.id = i.status_invoice_id" +
            " WHERE i.is_deleted = FALSE AND s.code = 'COMPLETED'" +
            " AND i.invoice_create_date >= :fromInstant AND i.invoice_create_date < :toInstant" +
            " AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)";

    @PersistenceContext
    private EntityManager entityManager;

    // [day, invoice count, revenue]
    public List<Object[]> revenueByDay(Instant from, Instant to, String warehouseId) {
        String day = LOCAL_DAY.formatted("i.invoice_create_date");
        return rows("SELECT " + day + " AS sale_day, COUNT(*), COALESCE(SUM(i.total_price), 0)" +
                COMPLETED_INVOICES + " GROUP BY sale_day", from, to, warehouseId);
    }

    // [day, cost of goods sold]
    public List<Object[]> costByDay(Instant from, Instant to, String warehouseId) {
        String day = LOCAL_DAY.formatted("i.invoice_create_date");
        return rows("SELECT " + day + " AS sale_day, COALESCE(SUM(d.input_price * d.quantity), 0)" +
                " FROM invoice_detail d JOIN invoice i ON i.id = d.invoice_id" +
                " JOIN status_invoice s ON s.id = i.status_invoice_id" +
                " WHERE i.is_deleted = FALSE AND s.code = 'COMPLETED'" +
                " AND i.invoice_create_date >= :fromInstant AND i.invoice_create_date < :toInstant" +
                " AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)" +
                " GROUP BY sale_day", from, to, warehouseId);
    }

    // [day, refunded amount, cost of the returned goods]
    public List<Object[]> returnsByDay(Instant from, Instant to, String warehouseId) {
        String day = LOCAL_DAY.formatted("r.return_date");
        return rows("SELECT " + day + " AS return_day, COALESCE(SUM(rd.line_total), 0)," +
                " COALESCE(SUM(rd.quantity * d.input_price), 0)" +
                " FROM sales_return_detail rd JOIN sales_return r ON r.id = rd.sales_return_id" +
                " JOIN status_invoice s ON s.id = r.status_invoice_id" +
                " JOIN invoice_detail d ON d.invoice_id = r.invoice_id AND d.product_id = rd.product_id" +
                " WHERE r.is_deleted = FALSE AND s.code = 'COMPLETED'" +
                " AND r.return_date >= :fromInstant AND r.return_date < :toInstant" +
                " AND (:warehouseId IS NULL OR r.warehouse_id = :warehouseId)" +
                " GROUP BY return_day", from, to, warehouseId);
    }

    // [product id, code, name, quantity, revenue, gross profit], best sellers first
    public List<Object[]> topProducts(Instant from, Instant to, String warehouseId, int limit) {
        Query query = query("SELECT p.id, p.code, p.name, SUM(d.quantity), SUM(d.line_total)," +
                " SUM(d.line_total - d.input_price * d.quantity)" +
                " FROM invoice_detail d JOIN invoice i ON i.id = d.invoice_id" +
                " JOIN status_invoice s ON s.id = i.status_invoice_id" +
                " JOIN product p ON p.id = d.product_id" +
                " WHERE i.is_deleted = FALSE AND s.code = 'COMPLETED'" +
                " AND i.invoice_create_date >= :fromInstant AND i.invoice_create_date < :toInstant" +
                " AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)" +
                " GROUP BY p.id, p.code, p.name ORDER BY SUM(d.quantity) DESC, SUM(d.line_total) DESC",
                from, to, warehouseId);
        query.setMaxResults(limit);
        return cast(query.getResultList());
    }

    // [employee id, code, name, invoice count, revenue]
    public List<Object[]> salesByEmployee(Instant from, Instant to, String warehouseId) {
        return rows("SELECT e.id, e.code, e.name, COUNT(*), COALESCE(SUM(i.total_price), 0)" +
                COMPLETED_INVOICES.replace(" WHERE", " JOIN employee e ON e.id = i.employee_id WHERE") +
                " GROUP BY e.id, e.code, e.name ORDER BY SUM(i.total_price) DESC", from, to, warehouseId);
    }

    // [warehouse id, warehouse name, product id, code, name, barcode, unit, quantity, min stock, cost, sale price]
    public List<Object[]> inventory(String warehouseId, boolean lowStockOnly) {
        Query query = entityManager.createNativeQuery("SELECT w.id, w.name, p.id, p.code, p.name, p.barcode, u.name," +
                " wp.available_quantity, p.min_stock, p.cost_price, p.sale_price" +
                " FROM warehouse_product wp JOIN warehouse w ON w.id = wp.warehouse_id" +
                " JOIN product p ON p.id = wp.product_id LEFT JOIN unit u ON u.id = p.unit_id" +
                " WHERE p.is_deleted = FALSE AND w.is_deleted = FALSE" +
                " AND (:warehouseId IS NULL OR wp.warehouse_id = :warehouseId)" +
                " AND (:lowStockOnly = FALSE OR wp.available_quantity <= p.min_stock)" +
                " ORDER BY w.name, p.name");
        query.setParameter("warehouseId", warehouseId);
        query.setParameter("lowStockOnly", lowStockOnly);
        return cast(query.getResultList());
    }

    // Every counted receipt line of products that have at least one dated lot in that warehouse, newest first:
    // [warehouse id, warehouse name, product id, code, name, lot, expiry date, quantity, stock-in code, on hand]
    public List<Object[]> receiptsOfDatedProducts(String warehouseId) {
        Query query = entityManager.createNativeQuery("SELECT si.warehouse_id, w.name, p.id, p.code, p.name," +
                " sd.lot_number, sd.expiry_date, sd.quantity, si.code, COALESCE(wp.available_quantity, 0)" +
                " FROM stock_in_detail sd JOIN stock_in si ON si.id = sd.stock_in_id" +
                " JOIN status_invoice s ON s.id = si.status_invoice_id" +
                " JOIN warehouse w ON w.id = si.warehouse_id JOIN product p ON p.id = sd.product_id" +
                " LEFT JOIN warehouse_product wp ON wp.warehouse_id = si.warehouse_id AND wp.product_id = sd.product_id" +
                " WHERE si.is_deleted = FALSE AND s.code <> 'CANCELLED' AND p.is_deleted = FALSE AND w.is_deleted = FALSE" +
                " AND (:warehouseId IS NULL OR si.warehouse_id = :warehouseId)" +
                " AND EXISTS (SELECT 1 FROM stock_in_detail xd JOIN stock_in xi ON xi.id = xd.stock_in_id" +
                "   WHERE xd.product_id = sd.product_id AND xi.warehouse_id = si.warehouse_id" +
                "   AND xd.expiry_date IS NOT NULL AND xi.is_deleted = FALSE)" +
                " ORDER BY si.warehouse_id, p.id, si.stock_in_date DESC, si.created_date DESC");
        query.setParameter("warehouseId", warehouseId);
        return cast(query.getResultList());
    }

    private List<Object[]> rows(String sql, Instant from, Instant to, String warehouseId) {
        return cast(query(sql, from, to, warehouseId).getResultList());
    }

    private Query query(String sql, Instant from, Instant to, String warehouseId) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromInstant", from);
        query.setParameter("toInstant", to);
        query.setParameter("warehouseId", warehouseId);
        return query;
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> cast(List<?> rows) {
        return (List<Object[]>) rows;
    }

}
