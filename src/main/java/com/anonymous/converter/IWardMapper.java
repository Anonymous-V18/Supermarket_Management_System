package com.anonymous.converter;

import com.anonymous.dto.response.WardResponse;
import com.anonymous.entity.Ward;
import org.mapstruct.Mapper;

@Mapper
public interface IWardMapper {

    WardResponse toDTO(Ward ward);

}
