package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.EmployeeSalesResponse;
import com.anonymous.dto.response.ExpiringLotResponse;
import com.anonymous.dto.response.InventoryRowResponse;
import com.anonymous.dto.response.ProductSalesResponse;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.service.IReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Dates are inclusive yyyy-MM-dd in Vietnam time; without them the last 30 days are used
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    IReportService reportService;

    @GetMapping("/revenue-by-day")
    @PreAuthorize(Permissions.REPORT)
    public ApiResponse<List<RevenueByDayResponse>> revenueByDay(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "warehouseId", required = false) String warehouseId
    ) {
        return ApiResponse.<List<RevenueByDayResponse>>builder()
                .result(reportService.revenueByDay(from, to, warehouseId))
                .build();
    }

    @GetMapping("/top-products")
    @PreAuthorize(Permissions.REPORT)
    public ApiResponse<List<ProductSalesResponse>> topProducts(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "warehouseId", required = false) String warehouseId,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.<List<ProductSalesResponse>>builder()
                .result(reportService.topProducts(from, to, warehouseId, limit))
                .build();
    }

    @GetMapping("/sales-by-employee")
    @PreAuthorize(Permissions.REPORT)
    public ApiResponse<List<EmployeeSalesResponse>> salesByEmployee(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "warehouseId", required = false) String warehouseId
    ) {
        return ApiResponse.<List<EmployeeSalesResponse>>builder()
                .result(reportService.salesByEmployee(from, to, warehouseId))
                .build();
    }

    @GetMapping("/inventory")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<InventoryRowResponse>> inventory(
            @RequestParam(value = "warehouseId", required = false) String warehouseId,
            @RequestParam(value = "lowStockOnly", defaultValue = "false") boolean lowStockOnly
    ) {
        return ApiResponse.<List<InventoryRowResponse>>builder()
                .result(reportService.inventory(warehouseId, lowStockOnly))
                .build();
    }

    @GetMapping("/expiring-lots")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<ExpiringLotResponse>> expiringLots(
            @RequestParam(value = "warehouseId", required = false) String warehouseId,
            @RequestParam(value = "withinDays", defaultValue = "30") int withinDays
    ) {
        return ApiResponse.<List<ExpiringLotResponse>>builder()
                .result(reportService.expiringLots(warehouseId, withinDays))
                .build();
    }

}
