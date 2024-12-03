package com.anonymous.converter;

import com.anonymous.dto.response.BrandResponse;
import com.anonymous.entity.Brand;
import org.mapstruct.Mapper;

@Mapper
public interface IBrandMapper {

    BrandResponse toDTO(Brand brand);

}
