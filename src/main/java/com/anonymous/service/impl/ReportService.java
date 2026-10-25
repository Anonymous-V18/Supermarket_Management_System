package com.anonymous.service.impl;

import com.anonymous.dto.response.EmployeeSalesResponse;
import com.anonymous.dto.response.ExpiringLotResponse;
import com.anonymous.dto.response.InventoryRowResponse;
import com.anonymous.dto.response.ProductSalesResponse;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.dto.response.WarehouseRevenueResponse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ReportRepository;
import com.anonymous.service.IReportService;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService implements IReportService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_TOP_PRODUCTS = 100;

    ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByDayResponse> revenueByDay(LocalDate from, LocalDate to, String warehouseId) {
        DateRange range = DateRange.of(from, to);
        String warehouse = blankToNull(warehouseId);
        Map<String, Object[]> sales = byDay(reportRepository.revenueByDay(range.start(), range.end(), warehouse));
        Map<String, Object[]> costs = byDay(reportRepository.costByDay(range.start(), range.end(), warehouse));
        Map<String, Object[]> returns = byDay(reportRepository.returnsByDay(range.start(), range.end(), warehouse));

        // One row per day of the range, zeros included, so charts have no gaps
        List<RevenueByDayResponse> days = new ArrayList<>();
        for (LocalDate day = range.from(); !day.isAfter(range.to()); day = day.plusDays(1)) {
            String key = day.toString();
            Object[] sale = sales.get(key);
            Object[] cost = costs.get(key);
            Object[] returned = returns.get(key);
            BigDecimal revenue = sale != null ? decimal(sale[2]) : BigDecimal.ZERO;
            BigDecimal refunds = returned != null ? decimal(returned[1]) : BigDecimal.ZERO;
            BigDecimal netCost = (cost != null ? decimal(cost[1]) : BigDecimal.ZERO)
                    .subtract(returned != null ? decimal(returned[2]) : BigDecimal.ZERO);
            BigDecimal netRevenue = revenue.subtract(refunds);
            days.add(RevenueByDayResponse.builder()
                    .date(day)
                    .invoiceCount(sale != null ? number(sale[1]) : 0)
                    .revenue(revenue)
                    .refunds(refunds)
                    .netRevenue(netRevenue)
                    .cost(netCost)
                    .grossProfit(netRevenue.subtract(netCost))
                    .build());
        }
        return days;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseRevenueResponse> revenueByWarehouse(LocalDate from, LocalDate to) {
        DateRange range = DateRange.of(from, to);
        List<Object[]> sales = reportRepository.revenueByWarehouse(range.start(), range.end());
        Map<String, Object[]> costs = byDay(reportRepository.costByWarehouse(range.start(), range.end()));
        Map<String, Object[]> returns = byDay(reportRepository.returnsByWarehouse(range.start(), range.end()));

        List<WarehouseRevenueResponse> result = new ArrayList<>();
        for (Object[] sale : sales) {
            String warehouseId = text(sale[0]);
            String warehouseName = text(sale[1]);
            long invoiceCount = number(sale[2]);
            BigDecimal revenue = decimal(sale[3]);

            Object[] costRow = costs.get(warehouseId);
            Object[] returnRow = returns.get(warehouseId);

            BigDecimal refunds = returnRow != null ? decimal(returnRow[1]) : BigDecimal.ZERO;
            BigDecimal netCost = (costRow != null ? decimal(costRow[1]) : BigDecimal.ZERO)
                    .subtract(returnRow != null ? decimal(returnRow[2]) : BigDecimal.ZERO);
            BigDecimal netRevenue = revenue.subtract(refunds);

            result.add(WarehouseRevenueResponse.builder()
                    .warehouseId(warehouseId)
                    .warehouseName(warehouseName)
                    .invoiceCount(invoiceCount)
                    .revenue(revenue)
                    .refunds(refunds)
                    .netRevenue(netRevenue)
                    .cost(netCost)
                    .grossProfit(netRevenue.subtract(netCost))
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSalesResponse> topProducts(LocalDate from, LocalDate to, String warehouseId, int limit) {
        DateRange range = DateRange.of(from, to);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_TOP_PRODUCTS);
        return reportRepository.topProducts(range.start(), range.end(), blankToNull(warehouseId), safeLimit).stream()
                .map(row -> ProductSalesResponse.builder()
                        .productId(text(row[0]))
                        .productCode(text(row[1]))
                        .productName(text(row[2]))
                        .quantity(number(row[3]))
                        .revenue(decimal(row[4]))
                        .grossProfit(decimal(row[5]))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalesResponse> salesByEmployee(LocalDate from, LocalDate to, String warehouseId) {
        DateRange range = DateRange.of(from, to);
        return reportRepository.salesByEmployee(range.start(), range.end(), blankToNull(warehouseId)).stream()
                .map(row -> EmployeeSalesResponse.builder()
                        .employeeId(text(row[0]))
                        .employeeCode(text(row[1]))
                        .employeeName(text(row[2]))
                        .invoiceCount(number(row[3]))
                        .revenue(decimal(row[4]))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryRowResponse> inventory(String warehouseId, boolean lowStockOnly) {
        return reportRepository.inventory(blankToNull(warehouseId), lowStockOnly).stream()
                .map(row -> {
                    int quantity = (int) number(row[7]);
                    int minStock = (int) number(row[8]);
                    BigDecimal costPrice = decimal(row[9]);
                    return InventoryRowResponse.builder()
                            .warehouseId(text(row[0]))
                            .warehouseName(text(row[1]))
                            .productId(text(row[2]))
                            .productCode(text(row[3]))
                            .productName(text(row[4]))
                            .barcode(text(row[5]))
                            .unitName(text(row[6]))
                            .quantity(quantity)
                            .minStock(minStock)
                            .costPrice(costPrice)
                            .salePrice(decimal(row[10]))
                            .stockValue(costPrice.multiply(BigDecimal.valueOf(quantity)))
                            .lowStock(quantity <= minStock)
                            .build();
                })
                .toList();
    }

    // Lots are not tracked per sale, so the stock on hand is attributed to the newest receipts
    // (goods leave first-in-first-out); a lot is listed while some of it is assumed to remain
    @Override
    @Transactional(readOnly = true)
    public List<ExpiringLotResponse> expiringLots(String warehouseId, int withinDays) {
        LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);
        LocalDate limit = today.plusDays(Math.min(Math.max(withinDays, 0), MAX_RANGE_DAYS));
        List<ExpiringLotResponse> lots = new ArrayList<>();
        Map<String, Integer> remaining = new HashMap<>();
        for (Object[] row : reportRepository.receiptsOfDatedProducts(blankToNull(warehouseId))) {
            String key = text(row[0]) + "|" + text(row[2]);
            int onHand = remaining.computeIfAbsent(key, k -> (int) number(row[9]));
            int fromThisLot = Math.min(onHand, (int) number(row[7]));
            remaining.put(key, onHand - fromThisLot);

            LocalDate expiryDate = localDate(row[6]);
            if (fromThisLot <= 0 || expiryDate == null || expiryDate.isAfter(limit)) {
                continue;
            }
            lots.add(ExpiringLotResponse.builder()
                    .warehouseId(text(row[0]))
                    .warehouseName(text(row[1]))
                    .productId(text(row[2]))
                    .productCode(text(row[3]))
                    .productName(text(row[4]))
                    .lotNumber(text(row[5]))
                    .expiryDate(expiryDate)
                    .daysLeft(ChronoUnit.DAYS.between(today, expiryDate))
                    .quantity(fromThisLot)
                    .stockInCode(text(row[8]))
                    .expired(expiryDate.isBefore(today))
                    .build());
        }
        lots.sort((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()));
        return lots;
    }

    // Inclusive local dates, converted to the UTC instants the columns store
    private record DateRange(LocalDate from, LocalDate to) {

        static DateRange of(LocalDate from, LocalDate to) {
            LocalDate end = to != null ? to : LocalDate.now(ShiftUtil.VIETNAM_ZONE);
            LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
            if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
                throw new AppException(ErrorCode.INVALID_DATE_RANGE);
            }
            return new DateRange(start, end);
        }

        Instant start() {
            return from.atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        }

        Instant end() {
            return to.plusDays(1).atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        }
    }

    private static Map<String, Object[]> byDay(List<Object[]> rows) {
        Map<String, Object[]> map = new HashMap<>();
        rows.forEach(row -> map.put(text(row[0]), row));
        return map;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String text(Object value) {
        return value != null ? value.toString() : null;
    }

    private static long number(Object value) {
        return value != null ? ((Number) value).longValue() : 0;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        return value instanceof Date date ? date.toLocalDate() : null;
    }

}
