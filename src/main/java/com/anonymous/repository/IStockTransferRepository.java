package com.anonymous.repository;

import com.anonymous.entity.StockTransfer;
import org.springframework.stereotype.Repository;

@Repository
public interface IStockTransferRepository extends ISoftDeleteRepository<StockTransfer> {
}
