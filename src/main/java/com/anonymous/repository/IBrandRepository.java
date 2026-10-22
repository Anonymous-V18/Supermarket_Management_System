package com.anonymous.repository;

import com.anonymous.entity.Brand;
import org.springframework.stereotype.Repository;

@Repository
public interface IBrandRepository extends ISoftDeleteRepository<Brand> {
}
