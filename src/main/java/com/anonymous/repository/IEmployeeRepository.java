package com.anonymous.repository;

import com.anonymous.entity.Employee;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.anonymous.entity.User;

@Repository
public interface IEmployeeRepository extends ISoftDeleteRepository<Employee> {

    Optional<Employee> findByCode(String code);

    Optional<Employee> findByUser(User user);
}
