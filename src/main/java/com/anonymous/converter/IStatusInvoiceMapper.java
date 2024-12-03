package com.anonymous.converter;

import com.anonymous.dto.response.StatusInvoiceResponse;
import com.anonymous.entity.StatusInvoice;
import org.mapstruct.Mapper;

@Mapper
public interface IStatusInvoiceMapper {

    StatusInvoiceResponse toDTO(StatusInvoice statusInvoice);

}
