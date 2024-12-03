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
public class StockOutInsertRequest extends StockOutRequest {

    String warehouseId;
    Set<StockOutDetailInsertRequest> stockOutDetailInsertRequests = new HashSet<>();

}
