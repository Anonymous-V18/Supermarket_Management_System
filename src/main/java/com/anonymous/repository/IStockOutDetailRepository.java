package com.anonymous.repository;

import com.anonymous.entity.StockOutDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockOutDetailRepository extends JpaRepository<StockOutDetail, String> {
}
