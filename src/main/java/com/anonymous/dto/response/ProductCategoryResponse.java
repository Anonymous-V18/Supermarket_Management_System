package com.anonymous.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCategoryResponse {

    String id;
    String name;
    String code;
    
}
