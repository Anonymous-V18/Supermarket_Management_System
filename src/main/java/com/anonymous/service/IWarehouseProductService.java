package com.anonymous.service;

import com.anonymous.entity.Product;
import com.anonymous.entity.Warehouse;

public interface IWarehouseProductService {

    void update(Product product, Warehouse warehouse, Integer oldQuantitySold, Integer newQuantitySold);

}
