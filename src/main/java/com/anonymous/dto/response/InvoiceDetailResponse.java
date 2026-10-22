package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceDetailResponse {

    String id;

    Integer quantity;

    BigDecimal inputPrice;

    BigDecimal salePrice;

    BigDecimal discountPercent;

    BigDecimal lineTotal;

    ProductResponse product;

}
