package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.constant.PaymentMethod;
import com.anonymous.converter.ISalesReturnMapper;
import com.anonymous.dto.request.DocumentLineRequest;
import com.anonymous.dto.request.SalesReturnInsertRequest;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Invoice;
import com.anonymous.entity.InvoiceDetail;
import com.anonymous.entity.Product;
import com.anonymous.entity.SalesReturn;
import com.anonymous.entity.SalesReturnDetail;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IInvoiceRepository;
import com.anonymous.repository.ISalesReturnDetailRepository;
import com.anonymous.repository.ISalesReturnRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesReturnServiceTest {

    @Mock ISalesReturnRepository salesReturnRepository;
    @Mock ISalesReturnDetailRepository salesReturnDetailRepository;
    @Mock IInvoiceRepository invoiceRepository;
    @Mock ISalesReturnMapper salesReturnMapper;
    @Mock IWarehouseProductService warehouseProductService;
    @Mock ICodeGeneratorService codeGeneratorService;
    @Mock DocumentSupport documentSupport;

    @InjectMocks
    SalesReturnService salesReturnService;

    Warehouse warehouse;
    Product milk;
    Product bread;
    StatusInvoice pending;
    StatusInvoice completed;
    Invoice invoice;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId("wh-1");
        milk = product("prd-milk");
        bread = product("prd-bread");
        pending = status("st-pending", DocumentStatus.PENDING);
        completed = status("st-completed", DocumentStatus.COMPLETED);

        // 2 x milk = 40 000, 1 x bread = 10 000, invoice discount 5 000 -> paid 45 000 (90 % of the lines)
        invoice = Invoice.builder().warehouse(warehouse).statusInvoice(completed)
                .paymentMethod(PaymentMethod.CARD).totalPrice(new BigDecimal("45000")).build();
        invoice.setId("inv-1");
        invoice.getInvoiceDetails().add(line(milk, 2, "40000"));
        invoice.getInvoiceDetails().add(line(bread, 1, "10000"));

        when(invoiceRepository.findForUpdate("inv-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdAndIsDeletedFalse("inv-1")).thenReturn(Optional.of(invoice));
        when(salesReturnRepository.sumReturnedByProduct("inv-1", null)).thenReturn(List.of());
        when(documentSupport.currentEmployee()).thenReturn(new Employee());
        when(documentSupport.statusForCreate(null)).thenReturn(pending);
        when(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_SALES_RETURN)).thenReturn("TH000001");
        when(salesReturnRepository.save(any(SalesReturn.class))).thenAnswer(call -> call.getArgument(0));
        when(documentSupport.stockQuantities(anyCollection(), anyString(), any(), any())).thenCallRealMethod();
        doCallRealMethod().when(documentSupport).checkNoDuplicateProducts(anyCollection(), any());
    }

    @Test
    void refundIncludesTheShareOfTheInvoiceDiscountAndGoodsGoBackToStock() {
        salesReturnService.insert(request(returnLine("prd-milk", 1)));

        SalesReturn saved = savedReturn();
        SalesReturnDetail detail = saved.getSalesReturnDetails().iterator().next();
        assertThat(detail.getLineTotal()).isEqualByComparingTo("18000");
        assertThat(detail.getUnitRefund()).isEqualByComparingTo("18000");
        assertThat(saved.getRefundAmount()).isEqualByComparingTo("18000");
        assertThat(saved.getTotalProduct()).isEqualTo(1);
        // Refunded the way the invoice was paid unless told otherwise
        assertThat(saved.getRefundMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(saved.getWarehouse()).isSameAs(warehouse);
        verify(warehouseProductService).applyChange(warehouse, Map.of(), Map.of(milk, 1), IWarehouseProductService.INCOMING);
    }

    @Test
    void returningEverythingRefundsExactlyWhatWasPaid() {
        salesReturnService.insert(request(returnLine("prd-milk", 2), returnLine("prd-bread", 1)));

        assertThat(savedReturn().getRefundAmount()).isEqualByComparingTo("45000");
    }

    @Test
    void cannotReturnMoreThanWhatIsLeftOnTheInvoice() {
        when(salesReturnRepository.sumReturnedByProduct("inv-1", null)).thenReturn(List.of(returned("prd-milk", 1)));

        assertError(() -> salesReturnService.insert(request(returnLine("prd-milk", 2))), ErrorCode.RETURN_QUANTITY_EXCEEDED);
        verify(warehouseProductService, never()).applyChange(any(), any(), any(), anyInt());
    }

    @Test
    void onlyProductsOnTheInvoiceCanBeReturned() {
        assertError(() -> salesReturnService.insert(request(returnLine("prd-other", 1))), ErrorCode.PRODUCT_NOT_IN_INVOICE);
    }

    @Test
    void onlyCompletedInvoicesCanHaveReturns() {
        invoice.setStatusInvoice(pending);

        assertError(() -> salesReturnService.insert(request(returnLine("prd-milk", 1))), ErrorCode.INVOICE_NOT_COMPLETED);
    }

    private SalesReturn savedReturn() {
        ArgumentCaptor<SalesReturn> captor = ArgumentCaptor.forClass(SalesReturn.class);
        verify(salesReturnRepository).save(captor.capture());
        return captor.getValue();
    }

    private static SalesReturnInsertRequest request(DocumentLineRequest... lines) {
        SalesReturnInsertRequest request = new SalesReturnInsertRequest();
        request.setInvoiceId("inv-1");
        request.setReason("Hàng lỗi");
        request.setLines(List.of(lines));
        return request;
    }

    private static DocumentLineRequest returnLine(String productId, int quantity) {
        DocumentLineRequest line = new DocumentLineRequest();
        line.setProductId(productId);
        line.setQuantity(quantity);
        return line;
    }

    private InvoiceDetail line(Product product, int quantity, String lineTotal) {
        return InvoiceDetail.builder().invoice(invoice).product(product).quantity(quantity)
                .lineTotal(new BigDecimal(lineTotal)).build();
    }

    private static ISalesReturnRepository.ReturnedQuantity returned(String productId, long quantity) {
        return new ISalesReturnRepository.ReturnedQuantity() {
            @Override
            public String getProductId() {
                return productId;
            }

            @Override
            public Long getQuantity() {
                return quantity;
            }
        };
    }

    private static Product product(String id) {
        Product product = new Product();
        product.setId(id);
        return product;
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
