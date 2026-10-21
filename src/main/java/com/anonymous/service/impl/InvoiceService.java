package com.anonymous.service.impl;

import com.anonymous.converter.IInvoiceMapper;
import com.anonymous.dto.request.InvoiceDetailUpdateRequest;
import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.*;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IInvoiceDetailService;
import com.anonymous.service.IInvoiceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceService implements IInvoiceService {

    IInvoiceRepository invoiceRepository;
    IInvoiceMapper invoiceMapper;
    ICustomerRepository customerRepository;
    IInvoiceDetailService invoiceDetailService;
    IAuthService authService;
    IEmployeeRepository employeeRepository;
    IWarehouseRepository warehouseRepository;
    IStatusInvoiceRepository statusInvoiceRepository;

    @Override
    @Transactional
    public void insert(InvoiceInsertRequest invoiceInsertRequest) {
        Customer customer = customerRepository.findById(invoiceInsertRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));

        String employeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));

        StatusInvoice statusInvoice = statusInvoiceRepository.findById(invoiceInsertRequest.getStatusInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));

        Warehouse warehouse = warehouseRepository.findById(invoiceInsertRequest.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));

        Invoice invoice = Invoice.builder()
                .discount(invoiceInsertRequest.getDiscount())
                .totalProduct(invoiceInsertRequest.getTotalProduct())
                .totalPrice(invoiceInsertRequest.getTotalPrice())
                .invoiceCreateDate(invoiceInsertRequest.getInvoiceCreateDate())
                .statusInvoice(statusInvoice)
                .customer(customer)
                .employee(employee)
                .warehouse(warehouse)
                .build();

        invoice = invoiceRepository.save(invoice);
        invoiceDetailService.insert(invoiceInsertRequest.getInvoiceDetailInsertRequests(), invoice);
    }

    @Override
    @Transactional
    public void update(String oldInvoiceId, InvoiceUpdateRequest invoiceUpdateRequest) {
        Invoice invoice = invoiceRepository.findById(oldInvoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));

        if (!invoice.getCustomer().getId().equals(invoiceUpdateRequest.getCustomerId())) {
            Customer customer = customerRepository.findById(invoiceUpdateRequest.getCustomerId())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));
            invoice.setCustomer(customer);
        }

        String employeeId = authService.getClaimsToken().get("sub").toString();
        if (!invoice.getEmployee().getId().equals(employeeId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!invoice.getStatusInvoice().getId().equals(invoiceUpdateRequest.getStatusInvoiceId())) {
            StatusInvoice statusInvoice = statusInvoiceRepository.findById(invoiceUpdateRequest.getStatusInvoiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));
            invoice.setStatusInvoice(statusInvoice);
        }

        String customerId = invoiceUpdateRequest.getCustomerId();
        if (!invoice.getCustomer().getId().equals(customerId)) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));
            invoice.setCustomer(customer);
        }

        invoice.setDiscount(invoiceUpdateRequest.getDiscount());
        invoice.setTotalProduct(invoiceUpdateRequest.getTotalProduct());
        invoice.setTotalPrice(invoiceUpdateRequest.getTotalPrice());
        invoice.setInvoiceCreateDate(invoiceUpdateRequest.getInvoiceCreateDate());

        invoice = invoiceRepository.save(invoice);

        invoiceDetailService.update(invoice.getInvoiceDetails(), invoiceUpdateRequest.getInvoiceDetailUpdateRequests());

        Set<String> updateInvoiceDetailIds = invoiceUpdateRequest.getInvoiceDetailUpdateRequests().stream()
                .map(InvoiceDetailUpdateRequest::getId)
                .collect(Collectors.toSet());
        if (invoice.getInvoiceDetails().size() != updateInvoiceDetailIds.size()) {
            Set<InvoiceDetail> deprecatedInvoiceDetails = invoice.getInvoiceDetails().stream()
                    .filter(invoiceDetail -> !updateInvoiceDetailIds.contains(invoiceDetail.getId()))
                    .collect(Collectors.toSet());
            invoiceDetailService.delete(deprecatedInvoiceDetails);
        }
        if (!invoiceUpdateRequest.getInvoiceDetailInsertRequests().isEmpty()) {
            invoiceDetailService.insert(invoiceUpdateRequest.getInvoiceDetailInsertRequests(), invoice);
        }

    }

    @Override
    @Transactional
    public void delete(String[] invoiceIds) {
        try {
            List<Invoice> invoices = invoiceRepository.findAllById(Set.of(invoiceIds));
            invoices.forEach(invoice -> invoiceDetailService.delete(invoice.getInvoiceDetails()));
            invoiceRepository.deleteAll(invoices);
        } catch (Exception e) {
            throw new AppException(ErrorCode.CAN_NOT_DELETE);
        }
    }

    @Override
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAll().stream().map(invoiceMapper::toDTO).toList();
    }

    @Override
    public InvoiceResponse getById(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_EXIST));
        return invoiceMapper.toDTO(invoice);
    }
}
