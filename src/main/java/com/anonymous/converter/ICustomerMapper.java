package com.anonymous.converter;

import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface ICustomerMapper {

    CustomerResponse toDTO(Customer customer);

    Customer toEntity(CustomerInsertRequest customerInsertRequest);

    Customer toEntity(@MappingTarget Customer oldCustomer, CustomerUpdateRequest customerUpdateRequest);
}
