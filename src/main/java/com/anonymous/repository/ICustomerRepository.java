package com.anonymous.repository;

import com.anonymous.entity.Customer;
import org.springframework.stereotype.Repository;

@Repository
public interface ICustomerRepository extends ISoftDeleteRepository<Customer> {
}
