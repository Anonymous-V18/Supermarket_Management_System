package com.anonymous.repository;

import com.anonymous.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUnitRepository extends JpaRepository<Unit, String> {
}
