package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.constant.PaymentMethod;
import com.anonymous.converter.IInvoiceMapper;
import com.anonymous.dto.request.InvoiceDetailInsertRequest;
import com.anonymous.dto.request.InvoiceDetailUpdateRequest;
import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.dto.response.InvoiceStatusCountResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.entity.Customer;
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
import org.springframework.data.jpa.domain.Specification;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IInvoiceService;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceService implements IInvoiceService {

    IInvoiceRepository invoiceRepository;
    IInvoiceDetailRepository invoiceDetailRepository;
    ISalesReturnRepository salesReturnRepository;
    IInvoiceMapper invoiceMapper;
    ICustomerRepository customerRepository;
    IWarehouseRepository warehouseRepository;
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;
    DocumentSupport documentSupport;

    @Override
    @Transactional
    public InvoiceResponse insert(InvoiceInsertRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
        documentSupport.checkNoDuplicateProducts(request.getInvoiceDetailInsertRequests(),
                InvoiceDetailInsertRequest::getProductId);

        Invoice invoice = Invoice.builder()
                .code(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_INVOICE))
                .invoiceCreateDate(Instant.now())
                .statusInvoice(documentSupport.statusForCreate(request.getStatusInvoiceId()))
                .customer(resolveCustomer(request.getCustomerId()))
                .employee(documentSupport.currentEmployee())
                .warehouse(warehouse)
                .build();
        request.getInvoiceDetailInsertRequests()
                .forEach(line -> invoice.getInvoiceDetails().add(newLine(invoice, line)));

        applyTotalsAndPayment(invoice, request);
        warehouseProductService.applyChange(warehouse, Map.of(), stockQuantities(invoice),
                IWarehouseProductService.OUTGOING);

        Invoice saved = invoiceRepository.save(invoice);
        invoiceDetailRepository.saveAll(saved.getInvoiceDetails());
        return invoiceMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse update(String invoiceId, InvoiceUpdateRequest request) {
        // Locked: a return being written for this invoice must finish first (see SalesReturnService)
        Invoice invoice = invoiceRepository.findForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));
        documentSupport.checkOwnerOrAdmin(invoice.getEmployee());

        Map<Product, Integer> stockBefore = stockQuantities(invoice);
        StatusInvoice currentStatus = invoice.getStatusInvoice();
        StatusInvoice nextStatus = documentSupport.statusForUpdate(currentStatus, request.getStatusInvoiceId());
        // Returned goods are already back in stock; cancelling the sale too would count them twice
        if (nextStatus != currentStatus && DocumentStatus.CANCELLED.equals(nextStatus.getCode())
                && salesReturnRepository.existsActiveByInvoiceId(invoice.getId())) {
            throw new AppException(ErrorCode.INVOICE_HAS_RETURNS);
        }

        if (DocumentStatus.isEditable(currentStatus.getCode())) {
            replaceLines(invoice, request);
            invoice.setCustomer(resolveCustomer(request.getCustomerId()));
            invoice.setStatusInvoice(nextStatus);
            applyTotalsAndPayment(invoice, request);
        } else if (nextStatus != currentStatus) {
            // Completed invoices only move to CANCELLED; their lines and amounts stay as they were
            invoice.setStatusInvoice(nextStatus);
        } else {
            throw new AppException(ErrorCode.DOCUMENT_NOT_EDITABLE);
        }

        warehouseProductService.applyChange(invoice.getWarehouse(), stockBefore, stockQuantities(invoice),
                IWarehouseProductService.OUTGOING);
        return invoiceMapper.toDTO(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public void delete(String[] invoiceIds) {
        // Soft delete: pending goods go back into stock, the invoice and its lines stay for auditing
        List<Invoice> invoices = invoiceRepository.findAllByIdInAndIsDeletedFalse(Set.of(invoiceIds));
        invoices.forEach(invoice -> {
            documentSupport.checkOwnerOrAdmin(invoice.getEmployee());
            documentSupport.checkDeletable(invoice.getStatusInvoice());
            warehouseProductService.applyChange(invoice.getWarehouse(), stockQuantities(invoice), Map.of(),
                    IWarehouseProductService.OUTGOING);
            invoice.markDeleted();
        });
        invoiceRepository.saveAll(invoices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAllByIsDeletedFalse().stream().map(invoiceMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(String keyword, String statusCode, Pageable pageable) {
        return search(keyword, null, null, statusCode, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(String keyword, LocalDate from, LocalDate to, String statusCode, Pageable pageable) {
        Specification<Invoice> spec = SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "customer.name", "customer.phoneNumber", "employee.name"),
                SearchSpecs.statusCode(statusCode)
        );
        if (from != null || to != null) {
            LocalDate startDay = from != null ? from : to;
            LocalDate endDay = to != null ? to : from;
            Instant startInstant = startDay.atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
            Instant endInstant = endDay.plusDays(1).atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
            Specification<Invoice> dateSpec = (root, query, cb) -> {
                var invoiceDate = cb.coalesce(root.<Instant>get("invoiceCreateDate"), root.<Instant>get("createdDate"));
                return cb.and(
                        cb.greaterThanOrEqualTo(invoiceDate, startInstant),
                        cb.lessThan(invoiceDate, endInstant)
                );
            };
            spec = spec.and(dateSpec);
        }
        return PageResponse.of(invoiceRepository.findAll(spec, pageable), invoiceMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));
        return invoiceMapper.toDTO(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceStatusCountResponse> countByStatus(LocalDate from, LocalDate to, String warehouseId) {
        Instant start = from.atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        return invoiceRepository.countByStatus(start, end, warehouseId).stream()
                .map(row -> InvoiceStatusCountResponse.builder()
                        .statusCode(row.getStatusCode())
                        .statusName(row.getStatusName())
                        .invoiceCount(row.getInvoiceCount())
                        .total(row.getTotal())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> findByDateRange(LocalDate from, LocalDate to, String warehouseId, int limit) {
        Instant start = from.atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant();
        int safeLimit = Math.clamp(limit, 1, 100);
        return invoiceRepository.findByDateRange(start, end, warehouseId, org.springframework.data.domain.PageRequest.of(0, safeLimit))
                .stream().map(invoiceMapper::toDTO).toList();
    }

    // Lines listed in the update keep their id; unlisted lines are removed; new lines are added
    private void replaceLines(Invoice invoice, InvoiceUpdateRequest request) {
        Map<String, InvoiceDetail> existing = invoice.getInvoiceDetails().stream()
                .collect(Collectors.toMap(InvoiceDetail::getId, Function.identity()));

        Set<InvoiceDetail> kept = new HashSet<>();
        for (InvoiceDetailUpdateRequest line : request.getInvoiceDetailUpdateRequests()) {
            InvoiceDetail detail = existing.get(line.getId());
            if (detail == null) {
                throw new AppException(ErrorCode.INVOICE_DETAIL_NOT_EXIST);
            }
            // Pending invoices are re-priced at the current price
            fillLine(detail, documentSupport.activeProduct(detail.getProduct().getId()),
                    line.getQuantity(), line.getDiscountPercent());
            kept.add(detail);
        }

        List<InvoiceDetail> removed = new ArrayList<>(invoice.getInvoiceDetails());
        removed.removeAll(kept);
        List<InvoiceDetail> added = request.getInvoiceDetailInsertRequests().stream()
                .map(line -> newLine(invoice, line))
                .toList();

        List<String> productIds = Stream.concat(kept.stream(), added.stream())
                .map(detail -> detail.getProduct().getId())
                .toList();
        documentSupport.checkNoDuplicateProducts(productIds, Function.identity());
        if (productIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        invoice.getInvoiceDetails().removeAll(removed);
        invoiceDetailRepository.deleteAll(removed);
        invoiceDetailRepository.flush();
        invoice.getInvoiceDetails().addAll(added);
        invoiceDetailRepository.saveAll(invoice.getInvoiceDetails());
    }

    private InvoiceDetail newLine(Invoice invoice, InvoiceDetailInsertRequest line) {
        InvoiceDetail detail = InvoiceDetail.builder().invoice(invoice).build();
        fillLine(detail, documentSupport.activeProduct(line.getProductId()), line.getQuantity(), line.getDiscountPercent());
        return detail;
    }

    // Prices come from the product, never from the client
    private void fillLine(InvoiceDetail detail, Product product, int quantity, BigDecimal discountPercent) {
        BigDecimal discount = MoneyUtil.orZero(discountPercent);
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setInputPrice(MoneyUtil.orZero(product.getCostPrice()));
        detail.setSalePrice(MoneyUtil.orZero(product.getSalePrice()));
        detail.setDiscountPercent(discount);
        detail.setLineTotal(MoneyUtil.lineTotal(product.getSalePrice(), quantity, discount));
    }

    private void applyTotalsAndPayment(Invoice invoice, InvoiceRequest request) {
        BigDecimal subtotal = invoice.getInvoiceDetails().stream()
                .map(InvoiceDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = MoneyUtil.round(request.getDiscount());
        if (discount.compareTo(subtotal) > 0) {
            throw new AppException(ErrorCode.DISCOUNT_EXCEEDS_TOTAL);
        }
        BigDecimal total = subtotal.subtract(discount);

        invoice.setTotalProduct(invoice.getInvoiceDetails().stream().mapToInt(InvoiceDetail::getQuantity).sum());
        invoice.setDiscount(discount);
        invoice.setTotalPrice(total);

        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH;
        invoice.setPaymentMethod(method);
        BigDecimal paid = request.getAmountPaid() != null ? MoneyUtil.round(request.getAmountPaid()) : null;
        if (paid == null && method != PaymentMethod.CASH) {
            // Card / transfer are always for the exact amount
            paid = total;
        }
        boolean completed = DocumentStatus.COMPLETED.equals(invoice.getStatusInvoice().getCode());
        if (completed && (paid == null || paid.compareTo(total) < 0)) {
            throw new AppException(ErrorCode.AMOUNT_PAID_NOT_ENOUGH);
        }
        invoice.setAmountPaid(paid);
        invoice.setChangeAmount(paid != null && paid.compareTo(total) >= 0 ? paid.subtract(total) : null);
    }

    private Map<Product, Integer> stockQuantities(Invoice invoice) {
        return documentSupport.stockQuantities(invoice.getInvoiceDetails(), invoice.getStatusInvoice().getCode(),
                InvoiceDetail::getProduct, InvoiceDetail::getQuantity);
    }

    // Empty customer = walk-in sale
    private Customer resolveCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        return customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));
    }

}
