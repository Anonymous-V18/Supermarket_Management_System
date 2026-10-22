package com.anonymous.repository;

import com.anonymous.entity.StockTakeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockTakeDetailRepository extends JpaRepository<StockTakeDetail, String> {
}
