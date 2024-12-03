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
public class StockInInsertRequest extends StockInRequest {

    String supplierId;
    String warehouseId;
    Set<StockInDetailInsertRequest> stockInDetailInsertRequests = new HashSet<>();

}
