package com.anonymous.repository;

import com.anonymous.entity.WarehouseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IWarehouseProductRepository extends JpaRepository<WarehouseProduct, String> {
}
