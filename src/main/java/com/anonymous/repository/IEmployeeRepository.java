package com.anonymous.repository;

import com.anonymous.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.anonymous.entity.User;

@Repository
public interface IEmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByCode(String code);

    Optional<Employee> findByUser(User user);

    Optional<Employee> findByUser_Username(String username);

    Optional<Employee> findByUser_Id(String userId);

    Optional<Employee> findByEmail(String email);
}
