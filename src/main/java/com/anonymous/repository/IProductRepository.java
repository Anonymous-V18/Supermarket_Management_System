package com.anonymous.repository;

import com.anonymous.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProductRepository extends ISoftDeleteRepository<Product> {

    List<Product> findByWarehouseProducts_Warehouse_Id(String id);

    // Barcodes are unique across all products, deleted ones included (database constraint)
    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findByBarcodeAndIsDeletedFalse(String barcode);

}
