package com.anonymous.dto.request;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {

    String name;
    String description;
    String details;
    Double vat = 0.0;
    Integer warranty;
    String image;
    String productCategoryId;
    String supplierId;
    String unitId;
    String brandId;

}
