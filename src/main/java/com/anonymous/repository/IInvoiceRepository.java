package com.anonymous.repository;

import com.anonymous.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IInvoiceRepository extends ISoftDeleteRepository<Invoice> {

    interface InvoiceStatusStats {
        String getStatusCode();

        Long getInvoiceCount();

        BigDecimal getRevenue();

        Long getProductCount();
    }

    interface InvoiceStatusCount {
        String getStatusCode();

        String getStatusName();

        Long getInvoiceCount();

        BigDecimal getTotal();
    }

    // Serializes returns against the same invoice (and cancelling it) so returned quantities can't be exceeded
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.isDeleted = false")
    Optional<Invoice> findForUpdate(@Param("id") String id);

    // employeeId = null summarizes every employee
    @Query("SELECT i.statusInvoice.code AS statusCode, COUNT(i) AS invoiceCount, " +
            "COALESCE(SUM(i.totalPrice), 0) AS revenue, COALESCE(SUM(i.totalProduct), 0) AS productCount " +
            "FROM Invoice i " +
            "WHERE i.isDeleted = false AND i.invoiceCreateDate >= :from AND i.invoiceCreateDate < :to " +
            "AND (:employeeId IS NULL OR i.employee.id = :employeeId) " +
            "GROUP BY i.statusInvoice.code")
    List<InvoiceStatusStats> summarizeByStatus(@Param("from") Instant from,
                                               @Param("to") Instant to,
                                               @Param("employeeId") String employeeId);

    // Every invoice of the period by status (whole store, as on the Invoices screen); warehouseId = null: every warehouse
    @Query("SELECT s.code AS statusCode, s.name AS statusName, COUNT(i) AS invoiceCount, " +
            "COALESCE(SUM(i.totalPrice), 0) AS total " +
            "FROM Invoice i JOIN i.statusInvoice s " +
            "WHERE i.isDeleted = false AND i.invoiceCreateDate >= :from AND i.invoiceCreateDate < :to " +
            "AND (:warehouseId IS NULL OR i.warehouse.id = :warehouseId) " +
            "GROUP BY s.code, s.name")
    List<InvoiceStatusCount> countByStatus(@Param("from") Instant from,
                                           @Param("to") Instant to,
                                           @Param("warehouseId") String warehouseId);

    @Query("SELECT i FROM Invoice i WHERE i.isDeleted = false " +
            "AND i.invoiceCreateDate >= :from AND i.invoiceCreateDate < :to " +
            "AND (:warehouseId IS NULL OR i.warehouse.id = :warehouseId) " +
            "ORDER BY i.invoiceCreateDate DESC")
    List<Invoice> findByDateRange(@Param("from") Instant from,
                                  @Param("to") Instant to,
                                  @Param("warehouseId") String warehouseId,
                                  Pageable pageable);

    @Query("SELECT DISTINCT i.createdBy FROM Invoice i WHERE i.invoiceCreateDate >= :from AND i.invoiceCreateDate < :to AND i.isDeleted = false")
    List<String> findDistinctCreatedByBetween(@Param("from") Instant from, @Param("to") Instant to);

}
