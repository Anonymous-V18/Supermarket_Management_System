package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    String id;
    String name;
    String description;
    String details;
    Double vat;
    Integer warranty;
    String image;
    ProductCategoryResponse productCategory;
    SupplierResponse supplier;
    UnitResponse unit;
    BrandResponse brand;
    
}
