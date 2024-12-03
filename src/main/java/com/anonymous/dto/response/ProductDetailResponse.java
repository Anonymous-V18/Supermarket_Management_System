package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ProductDetailResponse {

    String id;
    String name;
    Double vat;
    Integer warranty;
    String image;
    Double salePrice;
    UnitResponse unit;
    BrandResponse brand;

}
