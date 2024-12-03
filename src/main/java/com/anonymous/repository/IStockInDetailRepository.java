package com.anonymous.repository;

import com.anonymous.entity.StockInDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IStockInDetailRepository extends JpaRepository<StockInDetail, String> {
    @Query("""
            select s from StockInDetail s
            where s.stockIn.warehouse.id = ?1 and s.product.id = ?2
            order by s.stockIn.stockInDate DESC limit 1""")
    Optional<StockInDetail> getDetailProductInStockInDateNearest(String warehouseId, String productId);
}
