package com.anonymous.service.impl;


import com.anonymous.entity.Product;
import com.anonymous.entity.Warehouse;
import com.anonymous.entity.WarehouseProduct;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IWarehouseProductRepository;
import com.anonymous.service.IWarehouseProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseProductService implements IWarehouseProductService {

    IWarehouseProductRepository warehouseProductRepository;

    @Override
    public void update(Product product, Warehouse warehouse, Integer oldQuantitySold, Integer newQuantitySold) {
        WarehouseProduct warehouseProduct = product.getWarehouseProducts()
                .stream()
                .filter(p -> p.getWarehouse().getId().equals(warehouse.getId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_PRODUCT_NOT_EXIST));

        Integer availableQuantity = warehouseProduct.getAvailableQuantity() + oldQuantitySold - newQuantitySold;
        if (availableQuantity < 0) {
            throw new AppException(ErrorCode.STOCK_QUANTITY_INVALID);
        }
        warehouseProduct.setAvailableQuantity(availableQuantity);
        warehouseProductRepository.save(warehouseProduct);
    }

}
