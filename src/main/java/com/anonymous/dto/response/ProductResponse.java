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
public class ProductResponse {

    String id;

    String code;

    String barcode;

    String name;

    String description;

    String details;

    BigDecimal vat;

    Integer warranty;

    String image;

    BigDecimal salePrice;

    Integer minStock;

    ProductCategoryResponse productCategory;

    SupplierResponse supplier;

    UnitResponse unit;

    BrandResponse brand;

}
