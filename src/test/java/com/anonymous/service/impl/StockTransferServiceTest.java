package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.IStockTakeMapper;
import com.anonymous.converter.IStockTransferMapper;
import com.anonymous.dto.request.DocumentLineRequest;
import com.anonymous.dto.request.StockTakeInsertRequest;
import com.anonymous.dto.request.StockTakeLineRequest;
import com.anonymous.dto.request.StockTakeRequest;
import com.anonymous.dto.request.StockTransferInsertRequest;
import com.anonymous.dto.request.StockTransferRequest;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.StockTake;
import com.anonymous.entity.StockTransfer;
import com.anonymous.entity.StockTransferDetail;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IStockTakeDetailRepository;
import com.anonymous.repository.IStockTakeRepository;
import com.anonymous.repository.IStockTransferDetailRepository;
import com.anonymous.repository.IStockTransferRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IWarehouseProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// What each transfer status does to the source and destination warehouses
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockTransferServiceTest {

    @Mock IStockTransferRepository stockTransferRepository;
    @Mock IStockTransferDetailRepository stockTransferDetailRepository;
    @Mock IWarehouseRepository warehouseRepository;
    @Mock IStockTransferMapper stockTransferMapper;
    @Mock IWarehouseProductService warehouseProductService;
    @Mock ICodeGeneratorService codeGeneratorService;
    @Mock DocumentSupport documentSupport;

    @InjectMocks
    StockTransferService stockTransferService;

    Warehouse source = warehouse("wh-a");
    Warehouse destination = warehouse("wh-b");
    Product milk = new Product();
    StatusInvoice pending = status("st-pending", DocumentStatus.PENDING);
    StatusInvoice completed = status("st-completed", DocumentStatus.COMPLETED);
    StatusInvoice cancelled = status("st-cancelled", DocumentStatus.CANCELLED);

    @BeforeEach
    void setUp() {
        milk.setId("prd-milk");
        when(warehouseRepository.findByIdAndIsDeletedFalse("wh-a")).thenReturn(Optional.of(source));
        when(warehouseRepository.findByIdAndIsDeletedFalse("wh-b")).thenReturn(Optional.of(destination));
        when(documentSupport.currentEmployee()).thenReturn(new Employee());
        when(documentSupport.activeProduct("prd-milk")).thenReturn(milk);
        when(documentSupport.statusForCreate(null)).thenReturn(pending);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(call -> call.getArgument(0));
        when(documentSupport.stockQuantities(anyCollection(), anyString(), any(), any())).thenCallRealMethod();
        doCallRealMethod().when(documentSupport).checkNoDuplicateProducts(anyCollection(), any());
    }

    @Test
    void pendingTransferTakesGoodsOutOfTheSourceOnly() {
        stockTransferService.insert(request("wh-b"));

        verify(warehouseProductService).applyChange(source, Map.of(), Map.of(milk, -5), IWarehouseProductService.INCOMING);
        verify(warehouseProductService, never()).applyChange(org.mockito.ArgumentMatchers.eq(destination), any(), any(), anyInt());
    }

    @Test
    void completingTheTransferDeliversToTheDestination() {
        StockTransfer transfer = existing(pending);
        when(documentSupport.statusForUpdate(pending, "st-completed")).thenReturn(completed);

        stockTransferService.update("ck-1", update("st-completed"));

        assertThat(transfer.getStatusInvoice()).isSameAs(completed);
        verify(warehouseProductService).applyChange(destination, Map.of(), Map.of(milk, 5), IWarehouseProductService.INCOMING);
        verify(warehouseProductService).applyChange(source, Map.of(milk, -5), Map.of(milk, -5), IWarehouseProductService.INCOMING);
    }

    @Test
    void cancellingACompletedTransferUndoesBothSides() {
        existing(completed);
        when(documentSupport.statusForUpdate(completed, "st-cancelled")).thenReturn(cancelled);

        stockTransferService.update("ck-1", update("st-cancelled"));

        verify(warehouseProductService).applyChange(source, Map.of(milk, -5), Map.of(), IWarehouseProductService.INCOMING);
        verify(warehouseProductService).applyChange(destination, Map.of(milk, 5), Map.of(), IWarehouseProductService.INCOMING);
    }

    @Test
    void sourceAndDestinationMustDiffer() {
        assertError(() -> stockTransferService.insert(request("wh-a")), ErrorCode.SAME_WAREHOUSE_TRANSFER);
    }

    private StockTransfer existing(StatusInvoice status) {
        StockTransfer transfer = StockTransfer.builder().fromWarehouse(source).toWarehouse(destination)
                .statusInvoice(status).build();
        transfer.getStockTransferDetails().add(StockTransferDetail.builder()
                .stockTransfer(transfer).product(milk).quantity(5).build());
        when(stockTransferRepository.findByIdAndIsDeletedFalse("ck-1")).thenReturn(Optional.of(transfer));
        return transfer;
    }

    private StockTransferInsertRequest request(String toWarehouseId) {
        StockTransferInsertRequest request = new StockTransferInsertRequest();
        request.setFromWarehouseId("wh-a");
        request.setToWarehouseId(toWarehouseId);
        request.setLines(List.of(line()));
        return request;
    }

    private StockTransferRequest update(String statusId) {
        StockTransferRequest request = new StockTransferRequest();
        request.setToWarehouseId("wh-b");
        request.setStatusInvoiceId(statusId);
        request.setLines(List.of(line()));
        return request;
    }

    private DocumentLineRequest line() {
        DocumentLineRequest line = new DocumentLineRequest();
        line.setProductId("prd-milk");
        line.setQuantity(5);
        return line;
    }

    static Warehouse warehouse(String id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        return warehouse;
    }

    static StatusInvoice status(String id, String code) {
        StatusInvoice status = new StatusInvoice();
        status.setId(id);
        status.setCode(code);
        return status;
    }

    static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(errorCode);
    }

}
