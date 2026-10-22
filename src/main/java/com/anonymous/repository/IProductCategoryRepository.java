package com.anonymous.repository;

import com.anonymous.entity.ProductCategory;
import org.springframework.stereotype.Repository;

@Repository
public interface IProductCategoryRepository extends ISoftDeleteRepository<ProductCategory> {
}
