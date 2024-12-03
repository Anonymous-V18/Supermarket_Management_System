package com.anonymous.converter;

import com.anonymous.dto.response.StockInResponse;
import com.anonymous.entity.StockIn;
import org.mapstruct.Mapper;

@Mapper
public interface IStockInMapper {

    StockInResponse toDTO(StockIn stockIn);
    
}
