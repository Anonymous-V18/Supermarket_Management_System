package com.anonymous.repository;

import com.anonymous.entity.WarehouseProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IWarehouseProductRepository extends JpaRepository<WarehouseProduct, String> {

    // SELECT ... FOR UPDATE: two tills selling the same product wait for each other instead of overwriting stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wp FROM WarehouseProduct wp WHERE wp.warehouse.id = :warehouseId AND wp.product.id = :productId")
    Optional<WarehouseProduct> findForUpdate(@Param("warehouseId") String warehouseId, @Param("productId") String productId);

    Optional<WarehouseProduct> findByWarehouseIdAndProductId(String warehouseId, String productId);

}
