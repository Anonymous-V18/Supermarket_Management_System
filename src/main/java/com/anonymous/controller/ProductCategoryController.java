package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.ProductCategoryResponse;
import com.anonymous.service.IProductCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product-categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductCategoryController {

    IProductCategoryService productCategoryService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<ProductCategoryResponse>> showAll() {
        List<ProductCategoryResponse> response = productCategoryService.getAll();
        return ApiResponse.<List<ProductCategoryResponse>>builder()
                .result(response)
                .build();
    }

}
