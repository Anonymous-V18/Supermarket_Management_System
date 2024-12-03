package com.anonymous.converter;

import com.anonymous.dto.response.UnitResponse;
import com.anonymous.entity.Unit;
import org.mapstruct.Mapper;

@Mapper
public interface IUnitMapper {

    UnitResponse toDTO(Unit unit);

}
