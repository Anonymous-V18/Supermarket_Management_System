package com.anonymous.repository;

import com.anonymous.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByWarehouseProducts_Warehouse_Id(String id);

}
