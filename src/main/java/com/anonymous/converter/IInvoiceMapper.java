package com.anonymous.converter;

import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.entity.Invoice;
import org.mapstruct.Mapper;

@Mapper
public interface IInvoiceMapper {

    InvoiceResponse toDTO(Invoice invoice);

}
