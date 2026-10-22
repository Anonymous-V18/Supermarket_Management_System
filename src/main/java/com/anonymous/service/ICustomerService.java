package com.anonymous.service;

import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICustomerService {

    CustomerResponse insert(CustomerInsertRequest customerInsertRequest);

    CustomerResponse update(String oldCustomerId, CustomerUpdateRequest customerUpdateRequest);

    List<CustomerResponse> getAll();

    PageResponse<CustomerResponse> search(String keyword, Pageable pageable);

}
