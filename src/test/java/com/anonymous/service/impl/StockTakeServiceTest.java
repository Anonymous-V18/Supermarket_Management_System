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

// What each stock-take status does to warehouse stock
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockTakeServiceTest {

    @Mock IStockTakeRepository stockTakeRepository;
    @Mock IStockTakeDetailRepository stockTakeDetailRepository;
    @Mock IWarehouseRepository warehouseRepository;
    @Mock IStockTakeMapper stockTakeMapper;
    @Mock IWarehouseProductService warehouseProductService;
    @Mock ICodeGeneratorService codeGeneratorService;
    @Mock DocumentSupport documentSupport;

    @InjectMocks
    StockTakeService stockTakeService;

    Warehouse store = warehouse("wh-1");
    Product milk = new Product();
    StatusInvoice pending = status("st-pending", DocumentStatus.PENDING);
    StatusInvoice completed = status("st-completed", DocumentStatus.COMPLETED);

    @BeforeEach
    void setUp() {
        milk.setId("prd-milk");
        when(warehouseRepository.findByIdAndIsDeletedFalse("wh-1")).thenReturn(Optional.of(store));
        when(documentSupport.currentEmployee()).thenReturn(new Employee());
        when(documentSupport.activeProduct("prd-milk")).thenReturn(milk);
        when(documentSupport.statusForCreate(null)).thenReturn(pending);
        when(documentSupport.statusForCreate("st-completed")).thenReturn(completed);
        when(stockTakeRepository.save(any(StockTake.class))).thenAnswer(call -> call.getArgument(0));
        doCallRealMethod().when(documentSupport).checkNoDuplicateProducts(anyCollection(), any());
        when(warehouseProductService.getAvailableQuantity("wh-1", "prd-milk")).thenReturn(10);
        when(warehouseProductService.lockAvailableQuantity("wh-1", "prd-milk")).thenReturn(10);
    }

    @Test
    void pendingCountOnlyRecordsTheDifference() {
        stockTakeService.insert(request(null, 7));

        StockTake saved = saved();
        assertThat(saved.getTotalDifference()).isEqualTo(-3);
        assertThat(saved.getStockTakeDetails()).singleElement()
                .satisfies(detail -> assertThat(detail.getSystemQuantity()).isEqualTo(10));
        verify(warehouseProductService, never()).applyChange(any(), any(), any(), anyInt());
    }

    @Test
    void completedCountSetsStockToWhatWasCountedUnderALock() {
        stockTakeService.insert(request("st-completed", 7));

        verify(warehouseProductService).lockAvailableQuantity("wh-1", "prd-milk");
        verify(warehouseProductService).applyChange(store, Map.of(), Map.of(milk, -3), IWarehouseProductService.INCOMING);
    }

    @Test
    void completedCountIsFinal() {
        StockTake stockTake = StockTake.builder().warehouse(store).statusInvoice(completed).build();
        when(stockTakeRepository.findByIdAndIsDeletedFalse("kk-1")).thenReturn(Optional.of(stockTake));

        assertError(() -> stockTakeService.update("kk-1", new StockTakeRequest()), ErrorCode.DOCUMENT_NOT_EDITABLE);
    }

    private StockTake saved() {
        ArgumentCaptor<StockTake> captor = ArgumentCaptor.forClass(StockTake.class);
        verify(stockTakeRepository).save(captor.capture());
        return captor.getValue();
    }

    private StockTakeInsertRequest request(String statusId, int counted) {
        StockTakeLineRequest line = new StockTakeLineRequest();
        line.setProductId("prd-milk");
        line.setCountedQuantity(counted);
        StockTakeInsertRequest request = new StockTakeInsertRequest();
        request.setWarehouseId("wh-1");
        request.setStatusInvoiceId(statusId);
        request.setLines(List.of(line));
        return request;
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
