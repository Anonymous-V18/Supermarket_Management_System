package com.anonymous.converter;

import com.anonymous.dto.response.SalesReturnResponse;
import com.anonymous.entity.SalesReturn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ISalesReturnMapper {

    @Mapping(source = "invoice.id", target = "invoiceId")
    @Mapping(source = "invoice.code", target = "invoiceCode")
    SalesReturnResponse toDTO(SalesReturn entity);

}
