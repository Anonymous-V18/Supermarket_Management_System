package com.anonymous.converter;

import com.anonymous.dto.response.StockOutResponse;
import com.anonymous.entity.StockOut;
import org.mapstruct.Mapper;

@Mapper
public interface IStockOutMapper {

    StockOutResponse toDTO(StockOut stockOut);

}
