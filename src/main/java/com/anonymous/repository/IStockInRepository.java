package com.anonymous.repository;

import com.anonymous.entity.StockIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockInRepository extends JpaRepository<StockIn, String> {
}
