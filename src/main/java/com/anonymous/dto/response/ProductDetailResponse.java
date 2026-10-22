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
public class ProductDetailResponse {

    String id;

    String code;

    String barcode;

    String name;

    BigDecimal vat;

    Integer warranty;

    String image;

    BigDecimal salePrice;

    // Stock in the warehouse the detail was asked for
    Integer availableQuantity;

    UnitResponse unit;

    BrandResponse brand;

}
