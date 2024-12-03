package com.anonymous.converter;

import com.anonymous.dto.response.PositionResponse;
import com.anonymous.entity.Position;
import org.mapstruct.Mapper;

@Mapper
public interface IPositionMapper {

    PositionResponse toDTO(Position position);

}
