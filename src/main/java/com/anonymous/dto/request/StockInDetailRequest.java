package com.anonymous.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class StockInDetailRequest {

    Integer quantity = 0;
    Double inputPrice = 0.0;
    Double salePrice = 0.0;

}
