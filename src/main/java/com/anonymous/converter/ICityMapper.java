package com.anonymous.converter;

import com.anonymous.dto.response.CityResponse;
import com.anonymous.entity.City;
import org.mapstruct.Mapper;

@Mapper
public interface ICityMapper {

    CityResponse toDTO(City city);

}
