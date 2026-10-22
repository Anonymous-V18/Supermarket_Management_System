package com.anonymous.dto.request;

import jakarta.validation.Valid;
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

    // Lines kept (by id); lines not listed are removed
    @Valid
    Set<InvoiceDetailUpdateRequest> invoiceDetailUpdateRequests = new HashSet<>();

    @Valid
    Set<InvoiceDetailInsertRequest> invoiceDetailInsertRequests = new HashSet<>();

}
