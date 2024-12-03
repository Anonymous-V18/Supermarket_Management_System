package com.anonymous.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class InvoiceDetailRequest {

    Integer quantity = 0;
    Double promotionalPrice = 0.0;
    Double percentDiscount = 0.0;

}
