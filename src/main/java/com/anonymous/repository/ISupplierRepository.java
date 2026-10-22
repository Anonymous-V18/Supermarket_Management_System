package com.anonymous.repository;

import com.anonymous.entity.Supplier;
import org.springframework.stereotype.Repository;

@Repository
public interface ISupplierRepository extends ISoftDeleteRepository<Supplier> {
}
