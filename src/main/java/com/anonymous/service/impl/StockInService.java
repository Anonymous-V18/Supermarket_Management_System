package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.IStockInMapper;
import com.anonymous.dto.request.StockInDetailInsertRequest;
import com.anonymous.dto.request.StockInDetailUpdateRequest;
import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockInResponse;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.StockIn;
import com.anonymous.entity.StockInDetail;
import com.anonymous.entity.Supplier;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IProductRepository;
import com.anonymous.repository.IStockInDetailRepository;
import com.anonymous.repository.IStockInRepository;
import com.anonymous.repository.ISupplierRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IStockInService;
import com.anonymous.service.IWarehouseProductService;
import com.anonymous.utils.MoneyUtil;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockInService implements IStockInService {

    IStockInRepository stockInRepository;
    IStockInDetailRepository stockInDetailRepository;
    IStockInMapper stockInMapper;
    ISupplierRepository supplierRepository;
    IWarehouseRepository warehouseRepository;
    IProductRepository productRepository;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public StockInResponse insert(StockInInsertRequest request) {
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(request.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
        documentSupport.checkNoDuplicateProducts(request.getStockInDetailInsertRequests(),
                StockInDetailInsertRequest::getProductId);

        StockIn stockIn = StockIn.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_STOCK_IN))
                .stockInDate(request.getStockInDate() != null ? request.getStockInDate() : Instant.now())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .supplier(supplier)
                .warehouse(warehouse)
                .employee(documentSupport.currentEmployee())
                .build();
        request.getStockInDetailInsertRequests()
                .forEach(line -> stockIn.getStockInDetails().add(newLine(stockIn, line)));

        applyTotals(stockIn);
        updateProductPrices(stockIn);
        warehouseProductService.applyChange(warehouse, Map.of(), stockQuantities(stockIn),
                IWarehouseProductService.INCOMING);

        StockIn saved = stockInRepository.save(stockIn);
        stockInDetailRepository.saveAll(saved.getStockInDetails());
        return stockInMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public StockInResponse update(String stockInId, StockInUpdateRequest request) {
        StockIn stockIn = stockInRepository.findByIdAndIsDeletedFalse(stockInId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(stockIn.getEmployee());

        Map<Product, Integer> stockBefore = stockQuantities(stockIn);
        StatusInvoice currentStatus = stockIn.getStatusInvoice();
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());

        if (DocumentStatus.isEditable(currentStatus.getCode())) {
            replaceLines(stockIn, request);
            if (request.getStockInDate() != null) {
                stockIn.setStockInDate(request.getStockInDate());
            }
            stockIn.setStatusInvoice(nextStatus);
            applyTotals(stockIn);
            updateProductPrices(stockIn);
        } else if (nextStatus != currentStatus) {
            stockIn.setStatusInvoice(nextStatus);
        } else {
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }

        // Cancelling a receipt whose goods were already sold fails here (stock would go negative)
        warehouseProductService.applyChange(stockIn.getWarehouse(), stockBefore, stockQuantities(stockIn),
                IWarehouseProductService.INCOMING);
        return stockInMapper.toDTO(stockInRepository.save(stockIn));
    }

    @Override
    @Transactional
    public void delete(String[] stockInIds) {
        // Soft delete: received goods are taken back out of stock, the document stays for auditing
        List<StockIn> stockIns = stockInRepository.findAllByIdInAndIsDeletedFalse(Set.of(stockInIds));
        stockIns.forEach(stockIn -> {
            documentSupport.checkOwnerOrAdmin(stockIn.getEmployee());
            documentSupport.checkDeletable(stockIn.getStatusInvoice());
            warehouseProductService.applyChange(stockIn.getWarehouse(), stockQuantities(stockIn), Map.of(),
                    IWarehouseProductService.INCOMING);
            stockIn.markDeleted();
        });
        stockInRepository.saveAll(stockIns);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockInResponse> getAll() {
        return stockInRepository.findAllByIsDeletedFalse().stream().map(stockInMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockInResponse> search(String keyword, String statusCode, Pageable pageable) {
        return PageResponse.of(stockInRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "supplier.name", "warehouse.name", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        ), pageable), stockInMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public StockInResponse getById(String stockInId) {
        StockIn stockIn = stockInRepository.findByIdAndIsDeletedFalse(stockInId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_NOT_EXIST));
        return stockInMapper.toDTO(stockIn);
    }

    private void replaceLines(StockIn stockIn, StockInUpdateRequest request) {
        Map<String, StockInDetail> existing = stockIn.getStockInDetails().stream()
                .collect(Collectors.toMap(StockInDetail::getId, Function.identity()));

        Set<StockInDetail> kept = new HashSet<>();
        for (StockInDetailUpdateRequest line : request.getStockInDetailUpdateRequests()) {
            StockInDetail detail = existing.get(line.getId());
            if (detail == null) {
                throw new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST);
            }
            detail.setQuantity(line.getQuantity());
            detail.setInputPrice(MoneyUtil.round(line.getInputPrice()));
            detail.setSalePrice(MoneyUtil.round(line.getSalePrice()));
            if (!Objects.equals(detail.getExpiryDate(), line.getExpiryDate())) {
                checkExpiryDate(line.getExpiryDate());
            }
            detail.setLotNumber(trimToNull(line.getLotNumber()));
            detail.setExpiryDate(line.getExpiryDate());
            kept.add(detail);
        }

        List<StockInDetail> removed = new ArrayList<>(stockIn.getStockInDetails());
        removed.removeAll(kept);
        List<StockInDetail> added = request.getStockInDetailInsertRequests().stream()
                .map(line -> newLine(stockIn, line))
                .toList();

        List<String> productIds = Stream.concat(kept.stream(), added.stream())
                .map(detail -> detail.getProduct().getId())
                .toList();
        documentSupport.checkNoDuplicateProducts(productIds, Function.identity());
        if (productIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        stockIn.getStockInDetails().removeAll(removed);
        stockInDetailRepository.deleteAll(removed);
        stockInDetailRepository.flush();
        stockIn.getStockInDetails().addAll(added);
        stockInDetailRepository.saveAll(stockIn.getStockInDetails());
    }

    private StockInDetail newLine(StockIn stockIn, StockInDetailInsertRequest line) {
        checkExpiryDate(line.getExpiryDate());
        return StockInDetail.builder()
                .stockIn(stockIn)
                .product(documentSupport.activeProduct(line.getProductId()))
                .quantity(line.getQuantity())
                .inputPrice(MoneyUtil.round(line.getInputPrice()))
                .salePrice(MoneyUtil.round(line.getSalePrice()))
                .lotNumber(trimToNull(line.getLotNumber()))
                .expiryDate(line.getExpiryDate())
                .build();
    }

    // Expired goods can't be received; an unchanged date on an old line is left alone
    private void checkExpiryDate(LocalDate expiryDate) {
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now(ShiftUtil.VIETNAM_ZONE))) {
            throw new AppException(ErrorCode.EXPIRY_DATE_INVALID);
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void applyTotals(StockIn stockIn) {
        stockIn.setTotalProduct(stockIn.getStockInDetails().stream().mapToInt(StockInDetail::getQuantity).sum());
        stockIn.setTotalPrice(stockIn.getStockInDetails().stream()
                .map(detail -> detail.getInputPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // The latest receipt sets the product's purchase and retail price
    private void updateProductPrices(StockIn stockIn) {
        if (!DocumentStatus.affectsStock(stockIn.getStatusInvoice().getCode())) {
            return;
        }
        stockIn.getStockInDetails().forEach(detail -> {
            Product product = detail.getProduct();
            product.setCostPrice(detail.getInputPrice());
            product.setSalePrice(detail.getSalePrice());
            productRepository.save(product);
        });
    }

    private Map<Product, Integer> stockQuantities(StockIn stockIn) {
        return documentSupport.stockQuantities(stockIn.getStockInDetails(), stockIn.getStatusInvoice().getCode(),
                StockInDetail::getProduct, StockInDetail::getQuantity);
    }

}
