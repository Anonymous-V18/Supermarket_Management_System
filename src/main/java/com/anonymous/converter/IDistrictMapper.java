package com.anonymous.converter;

import com.anonymous.dto.response.DistrictResponse;
import com.anonymous.entity.District;
import org.mapstruct.Mapper;

@Mapper
public interface IDistrictMapper {

    DistrictResponse toDTO(District district);

}
