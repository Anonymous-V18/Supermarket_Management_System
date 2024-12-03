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
public class StockInResponse {

    String id;
    Integer totalProduct;
    Double totalPrice;
    Instant stockInDate;
    StatusInvoiceResponse statusInvoice;
    SupplierResponse supplier;
    WarehouseResponse warehouse;
    EmployeeResponse employee;
    Set<StockInDetailResponse> stockInDetails = new HashSet<>();

}
