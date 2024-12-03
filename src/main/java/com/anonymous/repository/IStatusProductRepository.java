package com.anonymous.repository;

import com.anonymous.entity.StatusProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStatusProductRepository extends JpaRepository<StatusProduct, String> {
}
