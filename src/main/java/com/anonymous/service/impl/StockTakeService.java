package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.IStockTakeMapper;
import com.anonymous.dto.request.StockTakeInsertRequest;
import com.anonymous.dto.request.StockTakeLineRequest;
import com.anonymous.dto.request.StockTakeRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTakeResponse;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.StockTake;
import com.anonymous.entity.StockTakeDetail;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IStockTakeDetailRepository;
import com.anonymous.repository.IStockTakeRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IStockTakeService;
import com.anonymous.service.IWarehouseProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A count sheet: while PENDING it only records counts; COMPLETED sets stock to the counted quantities and is final
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockTakeService implements IStockTakeService {

    IStockTakeRepository stockTakeRepository;
    IStockTakeDetailRepository stockTakeDetailRepository;
    IWarehouseRepository warehouseRepository;
    IStockTakeMapper stockTakeMapper;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public StockTakeResponse insert(StockTakeInsertRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
        documentSupport.checkNoDuplicateProducts(request.getLines(), StockTakeLineRequest::getProductId);

        StockTake stockTake = StockTake.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_STOCK_TAKE))
                .stockTakeDate(request.getStockTakeDate() != null ? request.getStockTakeDate() : Instant.now())
                .note(trimToNull(request.getNote()))
                .warehouse(warehouse)
                .employee(documentSupport.currentEmployee())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .build();
        addLines(stockTake, request.getLines());
        applyCountIfCompleted(stockTake);

        StockTake saved = stockTakeRepository.save(stockTake);
        stockTakeDetailRepository.saveAll(saved.getStockTakeDetails());
        return stockTakeMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public StockTakeResponse update(String stockTakeId, StockTakeRequest request) {
        StockTake stockTake = stockTakeRepository.findByIdAndIsDeletedFalse(stockTakeId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_TAKE_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(stockTake.getEmployee());

        StatusInvoice currentStatus = stockTake.getStatusInvoice();
        if (!DocumentStatus.isEditable(currentStatus.getCode())) {
            // A completed count is final (count again to correct it); a cancelled one is closed
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());
        documentSupport.checkNoDuplicateProducts(request.getLines(), StockTakeLineRequest::getProductId);

        stockTakeDetailRepository.deleteAll(stockTake.getStockTakeDetails());
        stockTakeDetailRepository.flush();
        stockTake.getStockTakeDetails().clear();

        stockTake.setStatusInvoice(nextStatus);
        stockTake.setNote(trimToNull(request.getNote()));
        if (request.getStockTakeDate() != null) {
            stockTake.setStockTakeDate(request.getStockTakeDate());
        }
        addLines(stockTake, request.getLines());
        applyCountIfCompleted(stockTake);

        stockTakeDetailRepository.saveAll(stockTake.getStockTakeDetails());
        return stockTakeMapper.toDTO(stockTakeRepository.save(stockTake));
    }

    @Override
    @Transactional
    public void delete(String[] stockTakeIds) {
        // Pending and cancelled counts never touched stock, so deleting them is only a soft delete
        List<StockTake> stockTakes = stockTakeRepository.findAllByIdInAndIsDeletedFalse(Set.of(stockTakeIds));
        stockTakes.forEach(stockTake -> {
            documentSupport.checkOwnerOrAdmin(stockTake.getEmployee());
            documentSupport.checkDeletable(stockTake.getStatusInvoice());
            stockTake.markDeleted();
        });
        stockTakeRepository.saveAll(stockTakes);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTakeResponse> search(String keyword, String statusCode, Pageable pageable) {
        return PageResponse.of(stockTakeRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "note", "warehouse.name", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        ), pageable), stockTakeMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTakeResponse getById(String stockTakeId) {
        StockTake stockTake = stockTakeRepository.findByIdAndIsDeletedFalse(stockTakeId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_TAKE_NOT_EXIST));
        return stockTakeMapper.toDTO(stockTake);
    }

    // The system quantity is read when the sheet is saved; on completion it is re-read under a row lock
    // (in product-id order, like WarehouseProductService) so sales made during the count are not lost
    private void addLines(StockTake stockTake, List<StockTakeLineRequest> lines) {
        boolean completing = DocumentStatus.COMPLETED.equals(stockTake.getStatusInvoice().getCode());
        String warehouseId = stockTake.getWarehouse().getId();
        lines.stream()
                .sorted(Comparator.comparing(StockTakeLineRequest::getProductId))
                .forEach(line -> {
                    Product product = documentSupport.activeProduct(line.getProductId());
                    int systemQuantity = completing
                            ? warehouseProductService.lockAvailableQuantity(warehouseId, product.getId())
                            : warehouseProductService.getAvailableQuantity(warehouseId, product.getId());
                    stockTake.getStockTakeDetails().add(StockTakeDetail.builder()
                            .stockTake(stockTake)
                            .product(product)
                            .systemQuantity(systemQuantity)
                            .countedQuantity(line.getCountedQuantity())
                            .difference(line.getCountedQuantity() - systemQuantity)
                            .build());
                });
        stockTake.setTotalDifference(stockTake.getStockTakeDetails().stream()
                .mapToInt(StockTakeDetail::getDifference).sum());
    }

    private void applyCountIfCompleted(StockTake stockTake) {
        if (!DocumentStatus.COMPLETED.equals(stockTake.getStatusInvoice().getCode())) {
            return;
        }
        Map<Product, Integer> differences = new LinkedHashMap<>();
        stockTake.getStockTakeDetails().stream()
                .filter(detail -> detail.getDifference() != 0)
                .forEach(detail -> differences.put(detail.getProduct(), detail.getDifference()));
        warehouseProductService.applyChange(stockTake.getWarehouse(), Map.of(), differences,
                IWarehouseProductService.INCOMING);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
