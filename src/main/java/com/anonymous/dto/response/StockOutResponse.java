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
public class StockOutResponse {

    String id;
    Integer totalProduct;
    Double totalPrice;
    String reason;
    Instant stockOutDate;
    StatusInvoiceResponse statusInvoice;
    SupplierResponse supplier;
    WarehouseResponse warehouse;
    EmployeeResponse employee;
    CustomerResponse customer;
    Set<StockOutDetailResponse> stockOutDetails = new HashSet<>();

}
