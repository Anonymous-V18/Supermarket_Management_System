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
public class StockTakeResponse {

    String id;

    String code;

    Instant createdDate;

    Instant stockTakeDate;

    String note;

    Integer totalDifference;

    StatusInvoiceResponse statusInvoice;

    WarehouseResponse warehouse;

    EmployeeResponse employee;

    Set<StockTakeDetailResponse> stockTakeDetails = new HashSet<>();

}
