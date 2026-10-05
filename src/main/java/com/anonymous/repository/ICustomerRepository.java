package com.anonymous.repository;

import com.anonymous.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByUser_Username(String username);

    Optional<Customer> findByUser_Id(String userId);

}
