package com.anonymous.repository;

import com.anonymous.entity.StockIn;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockInRepository extends ISoftDeleteRepository<StockIn> {
}
