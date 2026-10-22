package com.anonymous.repository;

import com.anonymous.entity.Warehouse;
import org.springframework.stereotype.Repository;

@Repository
public interface IWarehouseRepository extends ISoftDeleteRepository<Warehouse> {
}
