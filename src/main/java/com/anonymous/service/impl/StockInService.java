package com.anonymous.service.impl;

import com.anonymous.converter.IStockInMapper;
import com.anonymous.dto.request.StockInDetailUpdateRequest;
import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.StockInResponse;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.*;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IStockInDetailService;
import com.anonymous.service.IStockInService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockInService implements IStockInService {

    IStockInRepository stockInRepository;
    IStockInMapper stockInMapper;
    IStockInDetailService stockInDetailService;
    ISupplierRepository supplierRepository;
    IWarehouseRepository warehouseRepository;
    IStatusInvoiceRepository statusInvoiceRepository;
    IEmployeeRepository employeeRepository;
    IAuthService authService;

    @Override
    @Transactional
    public void insert(StockInInsertRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));

        StatusInvoice statusInvoice = statusInvoiceRepository.findById(request.getStatusInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));

        String employeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));

        StockIn stockIn = StockIn.builder()
                .totalProduct(request.getTotalProduct())
                .totalPrice(request.getTotalPrice())
                .stockInDate(request.getStockInDate())
                .statusInvoice(statusInvoice)
                .supplier(supplier)
                .warehouse(warehouse)
                .employee(employee)
                .build();

        stockIn = stockInRepository.save(stockIn);

        stockInDetailService.insert(request.getStockInDetailInsertRequests(), stockIn);
    }

    @Override
    @Transactional
    public void update(String oldStockInId, StockInUpdateRequest request) {
        StockIn stockIn = stockInRepository.findById(oldStockInId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_NOT_EXIST));

        if (!stockIn.getStatusInvoice().getId().equals(request.getStatusInvoiceId())) {
            StatusInvoice statusInvoice = statusInvoiceRepository.findById(request.getStatusInvoiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));
            stockIn.setStatusInvoice(statusInvoice);
        }

        String employeeId = authService.getClaimsToken().get("sub").toString();
        if (!stockIn.getEmployee().getId().equals(employeeId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        stockIn.setTotalProduct(request.getTotalProduct());
        stockIn.setTotalPrice(request.getTotalPrice());
        stockIn.setStockInDate(request.getStockInDate());

        stockIn = stockInRepository.save(stockIn);

        stockInDetailService.update(stockIn.getStockInDetails(), request.getStockInDetailUpdateRequests());

        Set<String> stockInIdsUpdated = request.getStockInDetailUpdateRequests().stream()
                .map(StockInDetailUpdateRequest::getId)
                .collect(Collectors.toSet());
        if (stockIn.getStockInDetails().size() != stockInIdsUpdated.size()) {
            Set<StockInDetail> deprecatedStockInDetails = stockIn.getStockInDetails().stream()
                    .filter(stockInDetail -> !stockInIdsUpdated.contains(stockInDetail.getId()))
                    .collect(Collectors.toSet());
            stockInDetailService.delete(deprecatedStockInDetails);
        }

        if (!request.getStockInDetailInsertRequests().isEmpty()) {
            stockInDetailService.insert(request.getStockInDetailInsertRequests(), stockIn);
        }

    }

    @Override
    @Transactional
    public void delete(String[] stockInIds) {
        try {
            List<StockIn> stockIns = stockInRepository.findAllById(Set.of(stockInIds));
            stockIns.forEach(stockIn -> stockInDetailService.delete(stockIn.getStockInDetails()));
            stockInRepository.deleteAll(stockIns);
        } catch (Exception e) {
            throw new AppException(ErrorCode.CAN_NOT_DELETE);
        }
    }

    @Override
    public List<StockInResponse> getAll() {
        return stockInRepository.findAll().stream().map(stockInMapper::toDTO).toList();
    }

    @Override
    public StockInResponse getById(String stockInId) {
        StockIn response = stockInRepository.findById(stockInId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_NOT_EXIST));
        return stockInMapper.toDTO(response);
    }

}
