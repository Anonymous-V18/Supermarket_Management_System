package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.constant.PaymentMethod;
import com.anonymous.converter.IInvoiceMapper;
import com.anonymous.dto.request.InvoiceDetailInsertRequest;
import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Invoice;
import com.anonymous.entity.InvoiceDetail;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICustomerRepository;
import com.anonymous.repository.IInvoiceDetailRepository;
import com.anonymous.repository.IInvoiceRepository;
import com.anonymous.repository.ISalesReturnRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IWarehouseProductService;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {

    @Mock IInvoiceRepository invoiceRepository;
    @Mock IInvoiceDetailRepository invoiceDetailRepository;
    @Mock ISalesReturnRepository salesReturnRepository;
    @Mock IInvoiceMapper invoiceMapper;
    @Mock ICustomerRepository customerRepository;
    @Mock IWarehouseRepository warehouseRepository;
    @Mock IWarehouseProductService warehouseProductService;
    @Mock ICodeGeneratorService codeGeneratorService;
    @Mock DocumentSupport documentSupport;

    @InjectMocks
    InvoiceService invoiceService;

    Warehouse warehouse;
    Product milk;
    StatusInvoice pending;
    StatusInvoice completed;
    StatusInvoice cancelled;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId("wh-1");
        milk = new Product();
        milk.setId("prd-milk");
        milk.setSalePrice(new BigDecimal("20000"));
        milk.setCostPrice(new BigDecimal("12000"));
        pending = status("st-pending", DocumentStatus.PENDING);
        completed = status("st-completed", DocumentStatus.COMPLETED);
        cancelled = status("st-cancelled", DocumentStatus.CANCELLED);

        when(warehouseRepository.findByIdAndIsDeletedFalse("wh-1")).thenReturn(Optional.of(warehouse));
        when(documentSupport.currentEmployee()).thenReturn(new Employee());
        when(documentSupport.activeProduct("prd-milk")).thenReturn(milk);
        when(documentSupport.statusForCreate(null)).thenReturn(pending);
        when(documentSupport.statusForCreate("st-completed")).thenReturn(completed);
        when(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_INVOICE)).thenReturn("HD000001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
        // Pure rules run for real
        when(documentSupport.stockQuantities(anyCollection(), anyString(), any(), any())).thenCallRealMethod();
        org.mockito.Mockito.doCallRealMethod().when(documentSupport).checkNoDuplicateProducts(anyCollection(), any());
        org.mockito.Mockito.doCallRealMethod().when(documentSupport).checkDeletable(any());
    }

    @Test
    void totalsAndPricesComeFromTheServerNotTheClient() {
        InvoiceInsertRequest request = insertRequest(line("prd-milk", 2, "10"));
        request.setDiscount(new BigDecimal("1000"));

        invoiceService.insert(request);

        Invoice saved = savedInvoice();
        InvoiceDetail detail = saved.getInvoiceDetails().iterator().next();
        assertThat(saved.getCode()).isEqualTo("HD000001");
        assertThat(detail.getSalePrice()).isEqualByComparingTo("20000");
        assertThat(detail.getInputPrice()).isEqualByComparingTo("12000");
        assertThat(detail.getLineTotal()).isEqualByComparingTo("36000");
        assertThat(saved.getTotalProduct()).isEqualTo(2);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("35000");
        assertThat(saved.getInvoiceCreateDate()).isNotNull();
        verify(warehouseProductService).applyChange(warehouse, Map.of(), Map.of(milk, 2), IWarehouseProductService.OUTGOING);
    }

    @Test
    void completedCashInvoiceNeedsEnoughMoneyAndGetsChange() {
        InvoiceInsertRequest notEnough = insertRequest(line("prd-milk", 2, "0"));
        notEnough.setStatusInvoiceId("st-completed");
        notEnough.setAmountPaid(new BigDecimal("30000"));
        assertError(() -> invoiceService.insert(notEnough), ErrorCode.AMOUNT_PAID_NOT_ENOUGH);

        InvoiceInsertRequest enough = insertRequest(line("prd-milk", 2, "0"));
        enough.setStatusInvoiceId("st-completed");
        enough.setAmountPaid(new BigDecimal("50000"));
        invoiceService.insert(enough);
        assertThat(savedInvoice().getChangeAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void cardPaymentIsForTheExactAmount() {
        InvoiceInsertRequest request = insertRequest(line("prd-milk", 1, "0"));
        request.setStatusInvoiceId("st-completed");
        request.setPaymentMethod(PaymentMethod.CARD);

        invoiceService.insert(request);

        assertThat(savedInvoice().getAmountPaid()).isEqualByComparingTo("20000");
        assertThat(savedInvoice().getChangeAmount()).isEqualByComparingTo("0");
    }

    @Test
    void discountCannotExceedTheGoodsTotal() {
        InvoiceInsertRequest request = insertRequest(line("prd-milk", 1, "0"));
        request.setDiscount(new BigDecimal("20001"));

        assertError(() -> invoiceService.insert(request), ErrorCode.DISCOUNT_EXCEEDS_TOTAL);
        verify(warehouseProductService, never()).applyChange(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void aProductAppearsOnceperInvoice() {
        InvoiceInsertRequest request = insertRequest(line("prd-milk", 1, "0"), line("prd-milk", 2, "0"));

        assertError(() -> invoiceService.insert(request), ErrorCode.DUPLICATE_PRODUCT_LINE);
    }

    @Test
    void completedInvoiceCannotBeEdited() {
        Invoice invoice = existingInvoice(completed);
        when(documentSupport.statusForUpdate(completed, null)).thenReturn(completed);

        assertError(() -> invoiceService.update("inv-1", new InvoiceUpdateRequest()), ErrorCode.DOCUMENT_NOT_EDITABLE);
        assertThat(invoice.getStatusInvoice()).isSameAs(completed);
    }

    @Test
    void cancellingACompletedInvoicePutsItsGoodsBack() {
        Invoice invoice = existingInvoice(completed);
        when(documentSupport.statusForUpdate(completed, "st-cancelled")).thenReturn(cancelled);
        InvoiceUpdateRequest request = new InvoiceUpdateRequest();
        request.setStatusInvoiceId("st-cancelled");

        invoiceService.update("inv-1", request);

        assertThat(invoice.getStatusInvoice()).isSameAs(cancelled);
        verify(warehouseProductService).applyChange(warehouse, Map.of(milk, 2), Map.of(), IWarehouseProductService.OUTGOING);
    }

    @Test
    void invoiceWithActiveReturnsCannotBeCancelled() {
        Invoice invoice = existingInvoice(completed);
        when(documentSupport.statusForUpdate(completed, "st-cancelled")).thenReturn(cancelled);
        when(salesReturnRepository.existsActiveByInvoiceId("inv-1")).thenReturn(true);
        InvoiceUpdateRequest request = new InvoiceUpdateRequest();
        request.setStatusInvoiceId("st-cancelled");

        assertError(() -> invoiceService.update("inv-1", request), ErrorCode.INVOICE_HAS_RETURNS);
        assertThat(invoice.getStatusInvoice()).isSameAs(completed);
        verify(warehouseProductService, never()).applyChange(any(), any(), any(), anyInt());
    }

    @Test
    void completedInvoiceIsCancelledNotDeleted() {
        Invoice invoice = existingInvoice(completed);
        when(invoiceRepository.findAllByIdInAndIsDeletedFalse(Set.of("inv-1"))).thenReturn(List.of(invoice));

        assertError(() -> invoiceService.delete(new String[]{"inv-1"}), ErrorCode.CANNOT_DELETE_COMPLETED);
        assertThat(invoice.getIsDeleted()).isFalse();
    }

    @Test
    void deletingAPendingInvoiceReturnsStockAndKeepsTheRow() {
        Invoice invoice = existingInvoice(pending);
        when(invoiceRepository.findAllByIdInAndIsDeletedFalse(Set.of("inv-1"))).thenReturn(List.of(invoice));

        invoiceService.delete(new String[]{"inv-1"});

        assertThat(invoice.getIsDeleted()).isTrue();
        assertThat(invoice.getDeletedDate()).isNotNull();
        verify(warehouseProductService).applyChange(warehouse, Map.of(milk, 2), Map.of(), IWarehouseProductService.OUTGOING);
        verify(invoiceDetailRepository, never()).deleteAll(any());
    }

    private Invoice savedInvoice() {
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private Invoice existingInvoice(StatusInvoice status) {
        Invoice invoice = Invoice.builder().warehouse(warehouse).statusInvoice(status).build();
        invoice.setId("inv-1");
        InvoiceDetail detail = InvoiceDetail.builder().invoice(invoice).product(milk).quantity(2)
                .salePrice(milk.getSalePrice()).inputPrice(milk.getCostPrice())
                .discountPercent(BigDecimal.ZERO).lineTotal(new BigDecimal("40000")).build();
        detail.setId("invd-1");
        invoice.getInvoiceDetails().add(detail);
        when(invoiceRepository.findByIdAndIsDeletedFalse("inv-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findForUpdate("inv-1")).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private static InvoiceInsertRequest insertRequest(InvoiceDetailInsertRequest... lines) {
        InvoiceInsertRequest request = new InvoiceInsertRequest();
        request.setWarehouseId("wh-1");
        request.setInvoiceDetailInsertRequests(new java.util.LinkedHashSet<>(List.of(lines)));
        return request;
    }

    private static InvoiceDetailInsertRequest line(String productId, int quantity, String discountPercent) {
        InvoiceDetailInsertRequest line = new InvoiceDetailInsertRequest();
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setDiscountPercent(new BigDecimal(discountPercent));
        return line;
    }

    private static StatusInvoice status(String id, String code) {
        StatusInvoice status = new StatusInvoice();
        status.setId(id);
        status.setCode(code);
        return status;
    }

    private static void assertError(ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(errorCode);
    }

}
