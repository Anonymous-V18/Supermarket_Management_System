package com.anonymous.service.impl;

import com.anonymous.converter.ICustomerMapper;
import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Customer;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICustomerRepository;
import com.anonymous.service.IAddressService;
import com.anonymous.service.ICustomerService;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

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

        customer.setAccumulatedPoints(0.0);

        customer = customerRepository.save(customer);
        return customerMapper.toDTO(customer);
    }

    @Override
    public CustomerResponse update(String oldCustomerId, CustomerUpdateRequest customerUpdateRequest) {
        Customer customer = customerRepository.findById(oldCustomerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_EXIST));

        customer = customerMapper.toEntity(customer, customerUpdateRequest);

        Address address = addressService.update(customerUpdateRequest.getCurrentAddress());
        customer.setAddress(address);

        customer = customerRepository.save(customer);
        return customerMapper.toDTO(customer);
    }

    @Override
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(customerMapper::toDTO).toList();
    }

}
