package com.anonymous.repository;

import com.anonymous.entity.StockTransferDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockTransferDetailRepository extends JpaRepository<StockTransferDetail, String> {
}
