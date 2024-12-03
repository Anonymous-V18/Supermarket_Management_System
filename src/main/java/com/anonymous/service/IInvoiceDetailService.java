package com.anonymous.service;

import com.anonymous.dto.request.InvoiceDetailInsertRequest;
import com.anonymous.dto.request.InvoiceDetailUpdateRequest;
import com.anonymous.entity.Invoice;
import com.anonymous.entity.InvoiceDetail;

import java.util.Set;

public interface IInvoiceDetailService {

    void insert(Set<InvoiceDetailInsertRequest> invoiceDetailInsertRequests, Invoice invoice);

    void update(Set<InvoiceDetail> oldInvoiceDetails, Set<InvoiceDetailUpdateRequest> invoiceDetailUpdateRequests);

    void delete(Set<InvoiceDetail> oldInvoiceDetails);
    
}
