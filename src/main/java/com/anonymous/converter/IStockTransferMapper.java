package com.anonymous.converter;

import com.anonymous.dto.response.StockTransferResponse;
import com.anonymous.entity.StockTransfer;
import org.mapstruct.Mapper;

@Mapper
public interface IStockTransferMapper {

    StockTransferResponse toDTO(StockTransfer entity);

}
