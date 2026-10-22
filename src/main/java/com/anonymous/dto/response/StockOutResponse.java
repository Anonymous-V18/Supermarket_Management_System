package com.anonymous.dto.response;

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
public class StockOutResponse {

    String id;

    String code;

    Instant createdDate;

    Integer totalProduct;

    BigDecimal totalPrice;

    String reason;

    Instant stockOutDate;

    StatusInvoiceResponse statusInvoice;

    WarehouseResponse warehouse;

    EmployeeResponse employee;

    CustomerResponse customer;

    Set<StockOutDetailResponse> stockOutDetails = new HashSet<>();

}
