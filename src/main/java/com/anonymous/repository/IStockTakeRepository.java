package com.anonymous.repository;

import com.anonymous.entity.StockTake;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockTakeRepository extends ISoftDeleteRepository<StockTake> {
}
