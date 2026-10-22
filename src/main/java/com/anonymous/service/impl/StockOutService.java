package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.IStockOutMapper;
import com.anonymous.dto.request.StockOutDetailInsertRequest;
import com.anonymous.dto.request.StockOutDetailUpdateRequest;
import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockOutResponse;
import com.anonymous.entity.Customer;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.StockOut;
import com.anonymous.entity.StockOutDetail;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICustomerRepository;
import com.anonymous.repository.IStockOutDetailRepository;
import com.anonymous.repository.IStockOutRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IStockOutService;
import com.anonymous.service.IWarehouseProductService;
import com.anonymous.utils.MoneyUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockOutService implements IStockOutService {

    IStockOutRepository stockOutRepository;
    IStockOutDetailRepository stockOutDetailRepository;
    IStockOutMapper stockOutMapper;
    ICustomerRepository customerRepository;
    IWarehouseRepository warehouseRepository;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public StockOutResponse insert(StockOutInsertRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
        documentSupport.checkNoDuplicateProducts(request.getStockOutDetailInsertRequests(),
                StockOutDetailInsertRequest::getProductId);

        StockOut stockOut = StockOut.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_STOCK_OUT))
                .reason(request.getReason().trim())
                .stockOutDate(request.getStockOutDate() != null ? request.getStockOutDate() : Instant.now())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .customer(resolveCustomer(request.getCustomerId()))
                .employee(documentSupport.currentEmployee())
                .warehouse(warehouse)
                .build();
        request.getStockOutDetailInsertRequests()
                .forEach(line -> stockOut.getStockOutDetails().add(newLine(stockOut, line)));

        applyTotals(stockOut);
        warehouseProductService.applyChange(warehouse, Map.of(), stockQuantities(stockOut),
                IWarehouseProductService.OUTGOING);

        StockOut saved = stockOutRepository.save(stockOut);
        stockOutDetailRepository.saveAll(saved.getStockOutDetails());
        return stockOutMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public StockOutResponse update(String stockOutId, StockOutUpdateRequest request) {
        StockOut stockOut = stockOutRepository.findByIdAndIsDeletedFalse(stockOutId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(stockOut.getEmployee());

        Map<Product, Integer> stockBefore = stockQuantities(stockOut);
        StatusInvoice currentStatus = stockOut.getStatusInvoice();
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());

        if (DocumentStatus.isEditable(currentStatus.getCode())) {
            replaceLines(stockOut, request);
            stockOut.setReason(request.getReason().trim());
            stockOut.setCustomer(resolveCustomer(request.getCustomerId()));
            if (request.getStockOutDate() != null) {
                stockOut.setStockOutDate(request.getStockOutDate());
            }
            stockOut.setStatusInvoice(nextStatus);
            applyTotals(stockOut);
        } else if (nextStatus != currentStatus) {
            stockOut.setStatusInvoice(nextStatus);
        } else {
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }

        warehouseProductService.applyChange(stockOut.getWarehouse(), stockBefore, stockQuantities(stockOut),
                IWarehouseProductService.OUTGOING);
        return stockOutMapper.toDTO(stockOutRepository.save(stockOut));
    }

    @Override
    @Transactional
    public void delete(String[] stockOutIds) {
        // Soft delete: pending goods go back into stock, the document and its lines stay for auditing
        List<StockOut> stockOuts = stockOutRepository.findAllByIdInAndIsDeletedFalse(Set.of(stockOutIds));
        stockOuts.forEach(stockOut -> {
            documentSupport.checkOwnerOrAdmin(stockOut.getEmployee());
            documentSupport.checkDeletable(stockOut.getStatusInvoice());
            warehouseProductService.applyChange(stockOut.getWarehouse(), stockQuantities(stockOut), Map.of(),
                    IWarehouseProductService.OUTGOING);
            stockOut.markDeleted();
        });
        stockOutRepository.saveAll(stockOuts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockOutResponse> getAll() {
        return stockOutRepository.findAllByIsDeletedFalse().stream().map(stockOutMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockOutResponse> search(String keyword, String statusCode, Pageable pageable) {
        return PageResponse.of(stockOutRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "reason", "warehouse.name", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        ), pageable), stockOutMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public StockOutResponse getById(String stockOutId) {
        StockOut stockOut = stockOutRepository.findByIdAndIsDeletedFalse(stockOutId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_NOT_EXIST));
        return stockOutMapper.toDTO(stockOut);
    }

    private void replaceLines(StockOut stockOut, StockOutUpdateRequest request) {
        Map<String, StockOutDetail> existing = stockOut.getStockOutDetails().stream()
                .collect(Collectors.toMap(StockOutDetail::getId, Function.identity()));

        Set<StockOutDetail> kept = new HashSet<>();
        for (StockOutDetailUpdateRequest line : request.getStockOutDetailUpdateRequests()) {
            StockOutDetail detail = existing.get(line.getId());
            if (detail == null) {
                throw new AppException(ErrorCode.STOCK_OUT_DETAIL_NOT_EXIST);
            }
            fillLine(detail, documentSupport.activeProduct(detail.getProduct().getId()), line.getQuantity());
            kept.add(detail);
        }

        List<StockOutDetail> removed = new ArrayList<>(stockOut.getStockOutDetails());
        removed.removeAll(kept);
        List<StockOutDetail> added = request.getStockOutDetailInsertRequests().stream()
                .map(line -> newLine(stockOut, line))
                .toList();

        List<String> productIds = Stream.concat(kept.stream(), added.stream())
                .map(detail -> detail.getProduct().getId())
                .toList();
        documentSupport.checkNoDuplicateProducts(productIds, Function.identity());
        if (productIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        stockOut.getStockOutDetails().removeAll(removed);
        stockOutDetailRepository.deleteAll(removed);
        stockOutDetailRepository.flush();
        stockOut.getStockOutDetails().addAll(added);
        stockOutDetailRepository.saveAll(stockOut.getStockOutDetails());
    }

    private StockOutDetail newLine(StockOut stockOut, StockOutDetailInsertRequest line) {
        StockOutDetail detail = StockOutDetail.builder().stockOut(stockOut).build();
        fillLine(detail, documentSupport.activeProduct(line.getProductId()), line.getQuantity());
        return detail;
    }

    // Goods leaving without a sale are valued at cost
    private void fillLine(StockOutDetail detail, Product product, int quantity) {
        BigDecimal costPrice = MoneyUtil.orZero(product.getCostPrice());
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setInputPrice(costPrice);
        detail.setSalePrice(MoneyUtil.orZero(product.getSalePrice()));
        detail.setTotalPrice(costPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    private void applyTotals(StockOut stockOut) {
        stockOut.setTotalProduct(stockOut.getStockOutDetails().stream().mapToInt(StockOutDetail::getQuantity).sum());
        stockOut.setTotalPrice(stockOut.getStockOutDetails().stream()
                .map(StockOutDetail::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Map<Product, Integer> stockQuantities(StockOut stockOut) {
        return documentSupport.stockQuantities(stockOut.getStockOutDetails(), stockOut.getStatusInvoice().getCode(),
                StockOutDetail::getProduct, StockOutDetail::getQuantity);
    }

    private Customer resolveCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        return customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));
    }

}
