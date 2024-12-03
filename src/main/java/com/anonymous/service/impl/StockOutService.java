package com.anonymous.service.impl;

import com.anonymous.converter.IStockOutMapper;
import com.anonymous.dto.request.StockOutDetailUpdateRequest;
import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.StockOutResponse;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.*;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IStockOutDetailService;
import com.anonymous.service.IStockOutService;
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
public class StockOutService implements IStockOutService {

    IStockOutRepository stockOutRepository;
    IStockOutMapper stockOutMapper;
    IStockOutDetailService stockOutDetailService;
    IWarehouseRepository warehouseRepository;
    IStatusInvoiceRepository statusInvoiceRepository;
    IEmployeeRepository employeeRepository;
    ICustomerRepository customerRepository;
    IAuthService authService;

    @Override
    @Transactional
    public void insert(StockOutInsertRequest stockOutInsertRequest) {
        Warehouse warehouse = warehouseRepository.findById(stockOutInsertRequest.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));

        StatusInvoice statusInvoice = statusInvoiceRepository.findById(stockOutInsertRequest.getStatusInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));

        String employeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));

        Customer customer = customerRepository.findById(stockOutInsertRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));

        StockOut stockOut = StockOut.builder()
                .totalProduct(stockOutInsertRequest.getTotalProduct())
                .totalPrice(stockOutInsertRequest.getTotalPrice())
                .reason(stockOutInsertRequest.getReason())
                .stockOutDate(stockOutInsertRequest.getStockOutDate())
                .statusInvoice(statusInvoice)
                .customer(customer)
                .employee(employee)
                .warehouse(warehouse)
                .build();

        stockOut = stockOutRepository.save(stockOut);
        stockOutDetailService.insert(stockOutInsertRequest.getStockOutDetailInsertRequests(), stockOut);
    }

    @Override
    public void update(String oldStockOutId, StockOutUpdateRequest stockOutUpdateRequest) {
        StockOut stockOut = stockOutRepository.findById(oldStockOutId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_NOT_EXIST));

        if (!stockOut.getStatusInvoice().getId().equals(stockOutUpdateRequest.getStatusInvoiceId())) {
            StatusInvoice statusInvoice = statusInvoiceRepository.findById(stockOutUpdateRequest.getStatusInvoiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));
            stockOut.setStatusInvoice(statusInvoice);
        }

        String employeeId = authService.getClaimsToken().get("sub").toString();
        if (!stockOut.getEmployee().getId().equals(employeeId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String customerId = stockOutUpdateRequest.getCustomerId();
        if (!stockOut.getCustomer().getId().equals(customerId)) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));
            stockOut.setCustomer(customer);
        }
        stockOut.setTotalProduct(stockOutUpdateRequest.getTotalProduct());
        stockOut.setTotalPrice(stockOutUpdateRequest.getTotalPrice());
        stockOut.setReason(stockOutUpdateRequest.getReason());
        stockOut.setStockOutDate(stockOutUpdateRequest.getStockOutDate());

        stockOutRepository.save(stockOut);

        stockOutDetailService.update(stockOut.getStockOutDetails(), stockOutUpdateRequest.getStockOutDetailUpdateRequests());

        Set<String> stockOutDetailIdsUpdated = stockOutUpdateRequest.getStockOutDetailUpdateRequests().stream()
                .map(StockOutDetailUpdateRequest::getId)
                .collect(Collectors.toSet());
        if (stockOut.getStockOutDetails().size() != stockOutDetailIdsUpdated.size()) {
            Set<StockOutDetail> deprecatedStockOutDetails = stockOut.getStockOutDetails().stream()
                    .filter(stockOutDetail -> !stockOutDetailIdsUpdated.contains(stockOutDetail.getId()))
                    .collect(Collectors.toSet());
            stockOutDetailService.delete(deprecatedStockOutDetails);
        }
        if (!stockOutUpdateRequest.getStockOutDetailInsertRequests().isEmpty()) {
            stockOutDetailService.insert(stockOutUpdateRequest.getStockOutDetailInsertRequests(), stockOut);
        }
    }

    @Override
    public void delete(String[] stockOutIds) {
        try {
            List<StockOut> stockOuts = stockOutRepository.findAllById(Set.of(stockOutIds));
            stockOuts.forEach(stockOut -> stockOutDetailService.delete(stockOut.getStockOutDetails()));
            stockOutRepository.deleteAll(stockOuts);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public List<StockOutResponse> getAll() {
        return stockOutRepository.findAll().stream().map(stockOutMapper::toDTO).toList();
    }

    @Override
    public StockOutResponse getById(String stockOutId) {
        StockOut response = stockOutRepository.findById(stockOutId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_NOT_EXIST));
        return stockOutMapper.toDTO(response);
    }

}
