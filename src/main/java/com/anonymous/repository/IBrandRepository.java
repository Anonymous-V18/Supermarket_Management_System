package com.anonymous.repository;

import com.anonymous.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBrandRepository extends JpaRepository<Brand, String> {
}
