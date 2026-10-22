package com.anonymous.dto.request;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOutUpdateRequest extends StockOutRequest {

    // Lines kept (by id); lines not listed are removed
    @Valid
    Set<StockOutDetailUpdateRequest> stockOutDetailUpdateRequests = new HashSet<>();

    @Valid
    Set<StockOutDetailInsertRequest> stockOutDetailInsertRequests = new HashSet<>();

}
