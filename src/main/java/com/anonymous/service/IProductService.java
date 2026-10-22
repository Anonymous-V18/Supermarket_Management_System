package com.anonymous.service;

import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProductService {

    ProductResponse insert(ProductRequest productRequest);

    ProductResponse update(String productId, ProductRequest productRequest);

    void delete(String[] productIds);

    List<ProductResponse> getAll();

    PageResponse<ProductResponse> search(String keyword, Pageable pageable);

    // Price and stock of a product in one warehouse, for the sales screen
    ProductDetailResponse getDetail(String productId, String warehouseId);

    ProductDetailResponse getByBarcode(String barcode, String warehouseId);

    ProductResponse getDetail(String productId);

}
