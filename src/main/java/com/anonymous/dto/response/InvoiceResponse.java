package com.anonymous.dto.response;

import com.anonymous.constant.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class InvoiceResponse {

    String id;

    String code;

    Instant createdDate;

    Instant modifiedDate;

    Instant invoiceCreateDate;

    BigDecimal discount;

    Integer totalProduct;

    BigDecimal totalPrice;

    PaymentMethod paymentMethod;

    BigDecimal amountPaid;

    BigDecimal changeAmount;

    StatusInvoiceResponse statusInvoice;

    Set<InvoiceDetailResponse> invoiceDetails = new HashSet<>();

    CustomerResponse customer;

    EmployeeResponse employee;

    WarehouseResponse warehouse;

}
