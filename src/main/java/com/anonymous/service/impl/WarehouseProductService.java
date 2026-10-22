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
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseProductService implements IWarehouseProductService {

    IWarehouseProductRepository warehouseProductRepository;

    @Override
    @Transactional
    public void applyChange(Warehouse warehouse, Map<Product, Integer> before, Map<Product, Integer> after, int direction) {
        Map<String, Product> products = new HashMap<>();
        Map<String, Integer> deltas = new HashMap<>();
        before.forEach((product, quantity) -> {
            products.put(product.getId(), product);
            deltas.merge(product.getId(), -direction * quantity, Integer::sum);
        });
        after.forEach((product, quantity) -> {
            products.put(product.getId(), product);
            deltas.merge(product.getId(), direction * quantity, Integer::sum);
        });

        // Lock rows in a fixed order so two documents touching the same products can't deadlock
        deltas.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> adjust(warehouse, products.get(entry.getKey()), entry.getValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(String warehouseId, String productId) {
        return warehouseProductRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(WarehouseProduct::getAvailableQuantity)
                .orElse(0);
    }

    @Override
    @Transactional
    public int lockAvailableQuantity(String warehouseId, String productId) {
        return warehouseProductRepository.findForUpdate(warehouseId, productId)
                .map(WarehouseProduct::getAvailableQuantity)
                .orElse(0);
    }

    private void adjust(Warehouse warehouse, Product product, int delta) {
        WarehouseProduct warehouseProduct = warehouseProductRepository
                .findForUpdate(warehouse.getId(), product.getId())
                .orElseGet(() -> {
                    if (delta < 0) {
                        throw new AppException(ErrorCode.STOCK_QUANTITY_INVALID);
                    }
                    // First receipt of this product in this warehouse
                    return WarehouseProduct.builder()
                            .warehouse(warehouse)
                            .product(product)
                            .availableQuantity(0)
                            .build();
                });

        int availableQuantity = warehouseProduct.getAvailableQuantity() + delta;
        if (availableQuantity < 0) {
            throw new AppException(ErrorCode.STOCK_QUANTITY_INVALID);
        }
        warehouseProduct.setAvailableQuantity(availableQuantity);
        warehouseProductRepository.save(warehouseProduct);
    }

}
