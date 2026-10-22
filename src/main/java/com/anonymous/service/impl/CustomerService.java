package com.anonymous.service.impl;

import com.anonymous.converter.ICustomerMapper;
import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Customer;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICustomerRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.IAddressService;
import com.anonymous.service.ICustomerService;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerService implements ICustomerService {

    ICustomerRepository customerRepository;
    ICustomerMapper customerMapper;
    IAddressService addressService;
    IUserService userService;

    @Override
    public CustomerResponse insert(CustomerInsertRequest customerInsertRequest) {
        Customer customer = customerMapper.toEntity(customerInsertRequest);

        User user = userService.insert(customerInsertRequest.getUserInsertRequest());
        customer.setUser(user);

        Address address = addressService.insert(customerInsertRequest.getNewAddress());
        customer.setAddress(address);

        customer.setAccumulatedPoints(0);

        customer = customerRepository.save(customer);
        return customerMapper.toDTO(customer);
    }

    @Override
    public CustomerResponse update(String oldCustomerId, CustomerUpdateRequest customerUpdateRequest) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(oldCustomerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));

        customer = customerMapper.toEntity(customer, customerUpdateRequest);

        Address address = addressService.update(customerUpdateRequest.getCurrentAddress());
        customer.setAddress(address);

        customer = customerRepository.save(customer);
        return customerMapper.toDTO(customer);
    }

    @Override
    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByIsDeletedFalse().stream().map(customerMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String keyword, Pageable pageable) {
        return PageResponse.of(customerRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "name", "phoneNumber", "email")
        ), pageable), customerMapper::toDTO);
    }

}
