package com.anonymous.repository;

import com.anonymous.entity.StockOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockOutRepository extends JpaRepository<StockOut, String> {
}
