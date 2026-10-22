package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.converter.ISalesReturnMapper;
import com.anonymous.dto.request.DocumentLineRequest;
import com.anonymous.dto.request.SalesReturnInsertRequest;
import com.anonymous.dto.request.SalesReturnRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.ReturnableLineResponse;
import com.anonymous.dto.response.SalesReturnResponse;
import com.anonymous.entity.Invoice;
import com.anonymous.entity.InvoiceDetail;
import com.anonymous.entity.Product;
import com.anonymous.entity.SalesReturn;
import com.anonymous.entity.SalesReturnDetail;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IInvoiceRepository;
import com.anonymous.repository.ISalesReturnDetailRepository;
import com.anonymous.repository.ISalesReturnRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.ISalesReturnService;
import com.anonymous.service.IWarehouseProductService;
import com.anonymous.utils.MoneyUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesReturnService implements ISalesReturnService {

    ISalesReturnRepository salesReturnRepository;
    ISalesReturnDetailRepository salesReturnDetailRepository;
    IInvoiceRepository invoiceRepository;
    ISalesReturnMapper salesReturnMapper;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public SalesReturnResponse insert(SalesReturnInsertRequest request) {
        Invoice invoice = lockInvoice(request.getInvoiceId());
        if (!DocumentStatus.COMPLETED.equals(invoice.getStatusInvoice().getCode())) {
            throw new AppException(ErrorCode.INVOICE_NOT_COMPLETED);
        }
        documentSupport.checkNoDuplicateProducts(request.getLines(), DocumentLineRequest::getProductId);

        SalesReturn salesReturn = SalesReturn.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_SALES_RETURN))
                .returnDate(request.getReturnDate() != null ? request.getReturnDate() : Instant.now())
                .reason(request.getReason().trim())
                .refundMethod(request.getRefundMethod() != null ? request.getRefundMethod() : invoice.getPaymentMethod())
                .invoice(invoice)
                .customer(invoice.getCustomer())
                .warehouse(invoice.getWarehouse())
                .employee(documentSupport.currentEmployee())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .build();
        addLines(salesReturn, request.getLines(), null);
        applyTotals(salesReturn);

        warehouseProductService.applyChange(salesReturn.getWarehouse(), Map.of(), stockQuantities(salesReturn),
                IWarehouseProductService.INCOMING);

        SalesReturn saved = salesReturnRepository.save(salesReturn);
        salesReturnDetailRepository.saveAll(saved.getSalesReturnDetails());
        return salesReturnMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public SalesReturnResponse update(String salesReturnId, SalesReturnRequest request) {
        SalesReturn salesReturn = salesReturnRepository.findByIdAndIsDeletedFalse(salesReturnId)
                .orElseThrow(() -> new AppException(ErrorCode.SALES_RETURN_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(salesReturn.getEmployee());
        lockInvoice(salesReturn.getInvoice().getId());

        Map<Product, Integer> stockBefore = stockQuantities(salesReturn);
        StatusInvoice currentStatus = salesReturn.getStatusInvoice();
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());

        if (DocumentStatus.isEditable(currentStatus.getCode())) {
            documentSupport.checkNoDuplicateProducts(request.getLines(), DocumentLineRequest::getProductId);
            salesReturnDetailRepository.deleteAll(salesReturn.getSalesReturnDetails());
            salesReturnDetailRepository.flush();
            salesReturn.getSalesReturnDetails().clear();
            addLines(salesReturn, request.getLines(), salesReturn.getId());
            salesReturnDetailRepository.saveAll(salesReturn.getSalesReturnDetails());

            salesReturn.setReason(request.getReason().trim());
            if (request.getRefundMethod() != null) {
                salesReturn.setRefundMethod(request.getRefundMethod());
            }
            if (request.getReturnDate() != null) {
                salesReturn.setReturnDate(request.getReturnDate());
            }
            salesReturn.setStatusInvoice(nextStatus);
            applyTotals(salesReturn);
        } else if (nextStatus != currentStatus) {
            // Completed returns can only be cancelled: the goods go back out of stock
            salesReturn.setStatusInvoice(nextStatus);
        } else {
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }

        warehouseProductService.applyChange(salesReturn.getWarehouse(), stockBefore, stockQuantities(salesReturn),
                IWarehouseProductService.INCOMING);
        return salesReturnMapper.toDTO(salesReturnRepository.save(salesReturn));
    }

    @Override
    @Transactional
    public void delete(String[] salesReturnIds) {
        // Soft delete of a pending return: the goods it put back are taken out again
        List<SalesReturn> salesReturns = salesReturnRepository.findAllByIdInAndIsDeletedFalse(Set.of(salesReturnIds));
        salesReturns.forEach(salesReturn -> {
            documentSupport.checkOwnerOrAdmin(salesReturn.getEmployee());
            documentSupport.checkDeletable(salesReturn.getStatusInvoice());
            warehouseProductService.applyChange(salesReturn.getWarehouse(), stockQuantities(salesReturn), Map.of(),
                    IWarehouseProductService.INCOMING);
            salesReturn.markDeleted();
        });
        salesReturnRepository.saveAll(salesReturns);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesReturnResponse> search(String keyword, String statusCode, Pageable pageable) {
        return PageResponse.of(salesReturnRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "reason", "invoice.code", "customer.name",
                        "customer.phoneNumber", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        ), pageable), salesReturnMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReturnResponse getById(String salesReturnId) {
        SalesReturn salesReturn = salesReturnRepository.findByIdAndIsDeletedFalse(salesReturnId)
                .orElseThrow(() -> new AppException(ErrorCode.SALES_RETURN_NOT_EXIST));
        return salesReturnMapper.toDTO(salesReturn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnableLineResponse> getReturnableLines(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));
        Map<String, Integer> returned = returnedQuantities(invoiceId, null);
        BigDecimal ratio = invoiceDiscountRatio(invoice);
        return invoice.getInvoiceDetails().stream()
                .sorted(Comparator.comparing(detail -> detail.getProduct().getName()))
                .map(detail -> {
                    int returnedQuantity = returned.getOrDefault(detail.getProduct().getId(), 0);
                    return ReturnableLineResponse.builder()
                            .productId(detail.getProduct().getId())
                            .productCode(detail.getProduct().getCode())
                            .productName(detail.getProduct().getName())
                            .soldQuantity(detail.getQuantity())
                            .returnedQuantity(returnedQuantity)
                            .returnableQuantity(Math.max(detail.getQuantity() - returnedQuantity, 0))
                            .unitRefund(unitRefund(refundFor(detail, 1, ratio), 1))
                            .build();
                })
                .toList();
    }

    // Each line is checked against what is left to return on the invoice (other active returns excluded)
    private void addLines(SalesReturn salesReturn, List<DocumentLineRequest> lines, String excludeReturnId) {
        Invoice invoice = salesReturn.getInvoice();
        Map<String, InvoiceDetail> sold = invoice.getInvoiceDetails().stream()
                .collect(Collectors.toMap(detail -> detail.getProduct().getId(), Function.identity()));
        Map<String, Integer> returned = returnedQuantities(invoice.getId(), excludeReturnId);
        BigDecimal ratio = invoiceDiscountRatio(invoice);

        for (DocumentLineRequest line : lines) {
            InvoiceDetail invoiceDetail = sold.get(line.getProductId());
            if (invoiceDetail == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_IN_INVOICE);
            }
            int returnable = invoiceDetail.getQuantity() - returned.getOrDefault(line.getProductId(), 0);
            if (line.getQuantity() > returnable) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }
            BigDecimal lineTotal = refundFor(invoiceDetail, line.getQuantity(), ratio);
            salesReturn.getSalesReturnDetails().add(SalesReturnDetail.builder()
                    .salesReturn(salesReturn)
                    .product(invoiceDetail.getProduct())
                    .quantity(line.getQuantity())
                    .unitRefund(unitRefund(lineTotal, line.getQuantity()))
                    .lineTotal(lineTotal)
                    .build());
        }
    }

    // Share of the line total the customer really paid: the invoice discount is spread over lines by value
    private static BigDecimal invoiceDiscountRatio(Invoice invoice) {
        BigDecimal subtotal = invoice.getInvoiceDetails().stream()
                .map(detail -> MoneyUtil.orZero(detail.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return MoneyUtil.orZero(invoice.getTotalPrice()).divide(subtotal, 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal refundFor(InvoiceDetail invoiceDetail, int quantity, BigDecimal ratio) {
        return MoneyUtil.round(MoneyUtil.orZero(invoiceDetail.getLineTotal())
                .multiply(ratio)
                .multiply(BigDecimal.valueOf(quantity))
                .divide(BigDecimal.valueOf(invoiceDetail.getQuantity()), 4, RoundingMode.HALF_UP));
    }

    private static BigDecimal unitRefund(BigDecimal lineTotal, int quantity) {
        return lineTotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Integer> returnedQuantities(String invoiceId, String excludeReturnId) {
        return salesReturnRepository.sumReturnedByProduct(invoiceId, excludeReturnId).stream()
                .collect(Collectors.toMap(ISalesReturnRepository.ReturnedQuantity::getProductId,
                        row -> row.getQuantity().intValue()));
    }

    private Invoice lockInvoice(String invoiceId) {
        return invoiceRepository.findForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));
    }

    private void applyTotals(SalesReturn salesReturn) {
        salesReturn.setTotalProduct(salesReturn.getSalesReturnDetails().stream()
                .mapToInt(SalesReturnDetail::getQuantity).sum());
        salesReturn.setRefundAmount(salesReturn.getSalesReturnDetails().stream()
                .map(SalesReturnDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Map<Product, Integer> stockQuantities(SalesReturn salesReturn) {
        return documentSupport.stockQuantities(salesReturn.getSalesReturnDetails(),
                salesReturn.getStatusInvoice().getCode(), SalesReturnDetail::getProduct, SalesReturnDetail::getQuantity);
    }

}
