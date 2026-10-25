package com.anonymous.service;

import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.dto.response.InvoiceStatusCountResponse;
import com.anonymous.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IInvoiceService {

    InvoiceResponse insert(InvoiceInsertRequest invoiceInsertRequest);

    InvoiceResponse update(String invoiceId, InvoiceUpdateRequest invoiceUpdateRequest);

    void delete(String[] invoiceIds);

    List<InvoiceResponse> getAll();

    PageResponse<InvoiceResponse> search(String keyword, String statusCode, Pageable pageable);

    PageResponse<InvoiceResponse> search(String keyword, LocalDate from, LocalDate to, String statusCode, Pageable pageable);

    InvoiceResponse getById(String invoiceId);

    // Invoices created from..to (inclusive days, Vietnam time) by status; warehouseId = null: every warehouse
    List<InvoiceStatusCountResponse> countByStatus(LocalDate from, LocalDate to, String warehouseId);

    List<InvoiceResponse> findByDateRange(LocalDate from, LocalDate to, String warehouseId, int limit);

}
