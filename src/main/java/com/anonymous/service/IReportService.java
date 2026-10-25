package com.anonymous.service;

import com.anonymous.dto.response.EmployeeSalesResponse;
import com.anonymous.dto.response.ExpiringLotResponse;
import com.anonymous.dto.response.InventoryRowResponse;
import com.anonymous.dto.response.ProductSalesResponse;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.dto.response.WarehouseRevenueResponse;

import java.time.LocalDate;
import java.util.List;

public interface IReportService {

    // from / to are inclusive Vietnam dates; null = the last 30 days
    List<RevenueByDayResponse> revenueByDay(LocalDate from, LocalDate to, String warehouseId);

    List<WarehouseRevenueResponse> revenueByWarehouse(LocalDate from, LocalDate to);

    List<ProductSalesResponse> topProducts(LocalDate from, LocalDate to, String warehouseId, int limit);

    List<EmployeeSalesResponse> salesByEmployee(LocalDate from, LocalDate to, String warehouseId);

    List<InventoryRowResponse> inventory(String warehouseId, boolean lowStockOnly);

    List<ExpiringLotResponse> expiringLots(String warehouseId, int withinDays);

}
