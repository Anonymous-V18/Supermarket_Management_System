package com.anonymous.service;

import com.anonymous.entity.Product;
import com.anonymous.entity.Warehouse;

import java.util.Map;

public interface IWarehouseProductService {

    int INCOMING = 1;
    int OUTGOING = -1;

    // Moves stock from the "before" quantities of a document to its "after" quantities.
    // direction INCOMING for stock-ins, OUTGOING for invoices and stock-outs.
    void applyChange(Warehouse warehouse, Map<Product, Integer> before, Map<Product, Integer> after, int direction);

    int getAvailableQuantity(String warehouseId, String productId);

    // Same as getAvailableQuantity but locks the row until the transaction ends (stock-take completion)
    int lockAvailableQuantity(String warehouseId, String productId);

}
