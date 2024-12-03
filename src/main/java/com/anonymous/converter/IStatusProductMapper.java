package com.anonymous.converter;

import com.anonymous.dto.response.StatusProductResponse;
import com.anonymous.entity.StatusProduct;
import org.mapstruct.Mapper;

@Mapper
public interface IStatusProductMapper {

    StatusProductResponse toDTO(StatusProduct statusProduct);

}
