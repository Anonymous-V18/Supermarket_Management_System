package com.anonymous.repository;

import com.anonymous.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface IRoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByCode(String code);

    Set<Role> findAllByCodeIn(Set<String> codes);
}
