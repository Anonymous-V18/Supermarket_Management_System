package com.anonymous.repository;

import com.anonymous.entity.StockOut;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockOutRepository extends ISoftDeleteRepository<StockOut> {
}
