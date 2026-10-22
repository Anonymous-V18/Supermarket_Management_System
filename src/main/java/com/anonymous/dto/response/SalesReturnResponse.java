package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.anonymous.constant.PaymentMethod;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesReturnResponse {

    String id;

    String code;

    Instant createdDate;

    Instant returnDate;

    String reason;

    Integer totalProduct;

    BigDecimal refundAmount;

    PaymentMethod refundMethod;

    String invoiceId;

    String invoiceCode;

    StatusInvoiceResponse statusInvoice;

    CustomerResponse customer;

    WarehouseResponse warehouse;

    EmployeeResponse employee;

    Set<SalesReturnDetailResponse> salesReturnDetails = new HashSet<>();

}
