package com.anonymous.converter;

import com.anonymous.dto.response.StockTakeResponse;
import com.anonymous.entity.StockTake;
import org.mapstruct.Mapper;

@Mapper
public interface IStockTakeMapper {

    StockTakeResponse toDTO(StockTake entity);

}
