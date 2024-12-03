package com.anonymous.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceUpdateRequest extends InvoiceRequest {

    Set<InvoiceDetailUpdateRequest> invoiceDetailUpdateRequests = new HashSet<>();
    Set<InvoiceDetailInsertRequest> invoiceDetailInsertRequests = new HashSet<>();

}
