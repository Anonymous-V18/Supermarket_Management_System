package com.anonymous.converter;

import com.anonymous.dto.response.ProductCategoryResponse;
import com.anonymous.entity.ProductCategory;
import org.mapstruct.Mapper;

@Mapper
public interface IProductCategoryMapper {

    ProductCategoryResponse toDTO(ProductCategory productCategory);

}
