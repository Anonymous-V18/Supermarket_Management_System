package com.anonymous.service;

import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;

import java.util.List;

public interface IProductService {

    ProductResponse insert(ProductRequest productRequest);

    ProductResponse update(String oldProductId, ProductRequest productRequest);

    void delete(String[] productIds);

    List<ProductResponse> getAll();

    ProductDetailResponse getDetail(String productId, String warehouseId);

    ProductResponse getDetail(String productId);
}
