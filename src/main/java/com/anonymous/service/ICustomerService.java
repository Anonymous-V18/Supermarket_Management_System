package com.anonymous.service;

import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.CustomerResponse;

import java.util.List;

public interface ICustomerService {

    CustomerResponse insert(CustomerInsertRequest customerInsertRequest);

    CustomerResponse update(String oldCustomerId, CustomerUpdateRequest customerUpdateRequest);

    List<CustomerResponse> getAll();

}
