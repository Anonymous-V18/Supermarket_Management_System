package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.IStockTransferMapper;
import com.anonymous.dto.request.DocumentLineRequest;
import com.anonymous.dto.request.StockTransferInsertRequest;
import com.anonymous.dto.request.StockTransferRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTransferResponse;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.StockTransfer;
import com.anonymous.entity.StockTransferDetail;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IStockTransferDetailRepository;
import com.anonymous.repository.IStockTransferRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IStockTransferService;
import com.anonymous.service.IWarehouseProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Stock leaves the source as soon as the transfer exists (PENDING = in transit) and reaches the
// destination when it is COMPLETED; cancelling undoes both sides
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockTransferService implements IStockTransferService {

    IStockTransferRepository stockTransferRepository;
    IStockTransferDetailRepository stockTransferDetailRepository;
    IWarehouseRepository warehouseRepository;
    IStockTransferMapper stockTransferMapper;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public StockTransferResponse insert(StockTransferInsertRequest request) {
        Warehouse fromWarehouse = activeWarehouse(request.getFromWarehouseId());
        Warehouse toWarehouse = activeWarehouse(request.getToWarehouseId());
        checkDifferentWarehouses(fromWarehouse, toWarehouse);
        documentSupport.checkNoDuplicateProducts(request.getLines(), DocumentLineRequest::getProductId);

        StockTransfer stockTransfer = StockTransfer.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_STOCK_TRANSFER))
                .transferDate(request.getTransferDate() != null ? request.getTransferDate() : Instant.now())
                .note(trimToNull(request.getNote()))
                .fromWarehouse(fromWarehouse)
                .toWarehouse(toWarehouse)
                .employee(documentSupport.currentEmployee())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .build();
        addLines(stockTransfer, request.getLines());

        moveStock(StockState.EMPTY, StockState.of(stockTransfer, documentSupport));

        StockTransfer saved = stockTransferRepository.save(stockTransfer);
        stockTransferDetailRepository.saveAll(saved.getStockTransferDetails());
        return stockTransferMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public StockTransferResponse update(String stockTransferId, StockTransferRequest request) {
        StockTransfer stockTransfer = stockTransferRepository.findByIdAndIsDeletedFalse(stockTransferId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_TRANSFER_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(stockTransfer.getEmployee());

        StockState before = StockState.of(stockTransfer, documentSupport);
        StatusInvoice currentStatus = stockTransfer.getStatusInvoice();
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());

        if (DocumentStatus.isEditable(currentStatus.getCode())) {
            Warehouse toWarehouse = activeWarehouse(request.getToWarehouseId());
            checkDifferentWarehouses(stockTransfer.getFromWarehouse(), toWarehouse);
            documentSupport.checkNoDuplicateProducts(request.getLines(), DocumentLineRequest::getProductId);

            stockTransferDetailRepository.deleteAll(stockTransfer.getStockTransferDetails());
            stockTransferDetailRepository.flush();
            stockTransfer.getStockTransferDetails().clear();
            addLines(stockTransfer, request.getLines());
            stockTransferDetailRepository.saveAll(stockTransfer.getStockTransferDetails());

            stockTransfer.setToWarehouse(toWarehouse);
            stockTransfer.setNote(trimToNull(request.getNote()));
            if (request.getTransferDate() != null) {
                stockTransfer.setTransferDate(request.getTransferDate());
            }
            stockTransfer.setStatusInvoice(nextStatus);
        } else if (nextStatus != currentStatus) {
            stockTransfer.setStatusInvoice(nextStatus);
        } else {
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }

        moveStock(before, StockState.of(stockTransfer, documentSupport));
        return stockTransferMapper.toDTO(stockTransferRepository.save(stockTransfer));
    }

    @Override
    @Transactional
    public void delete(String[] stockTransferIds) {
        // Soft delete of a pending transfer: the goods in transit go back to the source warehouse
        List<StockTransfer> stockTransfers = stockTransferRepository.findAllByIdInAndIsDeletedFalse(Set.of(stockTransferIds));
        stockTransfers.forEach(stockTransfer -> {
            documentSupport.checkOwnerOrAdmin(stockTransfer.getEmployee());
            documentSupport.checkDeletable(stockTransfer.getStatusInvoice());
            moveStock(StockState.of(stockTransfer, documentSupport), StockState.EMPTY);
            stockTransfer.markDeleted();
        });
        stockTransferRepository.saveAll(stockTransfers);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTransferResponse> search(String keyword, String statusCode, Pageable pageable) {
        return PageResponse.of(stockTransferRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "note", "fromWarehouse.name", "toWarehouse.name", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        ), pageable), stockTransferMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransferResponse getById(String stockTransferId) {
        StockTransfer stockTransfer = stockTransferRepository.findByIdAndIsDeletedFalse(stockTransferId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_TRANSFER_NOT_EXIST));
        return stockTransferMapper.toDTO(stockTransfer);
    }

    // What a transfer holds in each warehouse: goods out of the source, goods into the destination
    private record StockState(Warehouse from, Map<Product, Integer> outgoing, Warehouse to, Map<Product, Integer> incoming) {

        static final StockState EMPTY = new StockState(null, Map.of(), null, Map.of());

        static StockState of(StockTransfer transfer, DocumentSupport documentSupport) {
            String status = transfer.getStatusInvoice().getCode();
            Map<Product, Integer> outgoing = documentSupport.stockQuantities(transfer.getStockTransferDetails(), status,
                    StockTransferDetail::getProduct, StockTransferDetail::getQuantity);
            Map<Product, Integer> incoming = DocumentStatus.COMPLETED.equals(status) ? outgoing : Map.of();
            return new StockState(transfer.getFromWarehouse(), outgoing, transfer.getToWarehouse(), incoming);
        }
    }

    // Applies the difference between two states; warehouses are handled in id order to avoid lock cycles
    private void moveStock(StockState before, StockState after) {
        Map<String, Warehouse> warehouses = new LinkedHashMap<>();
        Map<String, Map<Product, Integer>> beforeByWarehouse = new LinkedHashMap<>();
        Map<String, Map<Product, Integer>> afterByWarehouse = new LinkedHashMap<>();
        collect(before.from(), before.outgoing(), -1, warehouses, beforeByWarehouse);
        collect(before.to(), before.incoming(), 1, warehouses, beforeByWarehouse);
        collect(after.from(), after.outgoing(), -1, warehouses, afterByWarehouse);
        collect(after.to(), after.incoming(), 1, warehouses, afterByWarehouse);

        List<String> warehouseIds = new ArrayList<>(warehouses.keySet());
        warehouseIds.sort(Comparator.naturalOrder());
        warehouseIds.forEach(warehouseId -> warehouseProductService.applyChange(warehouses.get(warehouseId),
                beforeByWarehouse.getOrDefault(warehouseId, Map.of()),
                afterByWarehouse.getOrDefault(warehouseId, Map.of()),
                IWarehouseProductService.INCOMING));
    }

    // Quantities are signed (+ into the warehouse, - out of it) so both sides fit one INCOMING call
    private static void collect(Warehouse warehouse, Map<Product, Integer> quantities, int sign,
                                Map<String, Warehouse> warehouses, Map<String, Map<Product, Integer>> target) {
        if (warehouse == null || quantities.isEmpty()) {
            return;
        }
        warehouses.put(warehouse.getId(), warehouse);
        Map<Product, Integer> signed = target.computeIfAbsent(warehouse.getId(), id -> new LinkedHashMap<>());
        quantities.forEach((product, quantity) -> signed.merge(product, sign * quantity, Integer::sum));
    }

    private void addLines(StockTransfer stockTransfer, List<DocumentLineRequest> lines) {
        lines.forEach(line -> stockTransfer.getStockTransferDetails().add(StockTransferDetail.builder()
                .stockTransfer(stockTransfer)
                .product(documentSupport.activeProduct(line.getProductId()))
                .quantity(line.getQuantity())
                .build()));
        stockTransfer.setTotalProduct(stockTransfer.getStockTransferDetails().stream()
                .mapToInt(StockTransferDetail::getQuantity).sum());
    }

    private Warehouse activeWarehouse(String warehouseId) {
        return warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
    }

    private static void checkDifferentWarehouses(Warehouse from, Warehouse to) {
        if (from.getId().equals(to.getId())) {
            throw new AppException(ErrorCode.SAME_WAREHOUSE_TRANSFER);
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
