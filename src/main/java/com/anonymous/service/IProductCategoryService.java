package com.anonymous.service;

import com.anonymous.dto.response.ProductCategoryResponse;

import java.util.List;

public interface IProductCategoryService {

    List<ProductCategoryResponse> getAll();

}
