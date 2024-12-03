package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceResponse {

    String id;
    Instant createdDate;
    Instant modifiedDate;
    Instant invoiceCreateDate;
    Double discount;
    Integer totalProduct;
    Double totalPrice;
    StatusInvoiceResponse statusInvoice;
    Set<InvoiceDetailResponse> invoiceDetails = new HashSet<>();
    CustomerResponse customer;
    EmployeeResponse employee;
    WarehouseResponse warehouse;

}
