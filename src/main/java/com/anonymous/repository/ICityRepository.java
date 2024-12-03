package com.anonymous.repository;

import com.anonymous.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICityRepository extends JpaRepository<City, String> {
}
