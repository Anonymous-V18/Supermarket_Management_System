package com.anonymous.service;

import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.InvoiceResponse;

import java.util.List;

public interface IInvoiceService {

    void insert(InvoiceInsertRequest invoiceInsertRequest);

    void update(String oldInvoiceId, InvoiceUpdateRequest invoiceUpdateRequest);

    void delete(String[] invoiceIds);

    List<InvoiceResponse> getAll();

    InvoiceResponse getById(String invoiceId);
}
