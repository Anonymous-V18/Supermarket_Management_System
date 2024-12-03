package com.anonymous.service.impl;

import com.anonymous.converter.IProductCategoryMapper;
import com.anonymous.dto.response.ProductCategoryResponse;
import com.anonymous.repository.IProductCategoryRepository;
import com.anonymous.service.IProductCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductCategoryService implements IProductCategoryService {

    IProductCategoryRepository productCategoryRepository;
    IProductCategoryMapper productCategoryMapper;

    @Override
    public List<ProductCategoryResponse> getAll() {
        return productCategoryRepository.findAll().stream().map(productCategoryMapper::toDTO).toList();
    }

}
