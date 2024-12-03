package com.anonymous.converter;

import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface IProductMapper {

    ProductResponse toDTO(Product product);

    ProductDetailResponse toCommonDetailDTO(Product product);

    @Mapping(target = "warehouseProducts", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "stockOutDetails", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "productCategory", ignore = true)
    @Mapping(target = "invoiceDetails", ignore = true)
    @Mapping(target = "brand", ignore = true)
    Product toEntity(ProductRequest productRequest);

    Product toEntity(@MappingTarget Product oldProduct, ProductRequest productRequest);
}
