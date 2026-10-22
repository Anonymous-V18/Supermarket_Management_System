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
public class StockTransferResponse {

    String id;

    String code;

    Instant createdDate;

    Instant transferDate;

    String note;

    Integer totalProduct;

    StatusInvoiceResponse statusInvoice;

    WarehouseResponse fromWarehouse;

    WarehouseResponse toWarehouse;

    EmployeeResponse employee;

    Set<StockTransferDetailResponse> stockTransferDetails = new HashSet<>();

}
