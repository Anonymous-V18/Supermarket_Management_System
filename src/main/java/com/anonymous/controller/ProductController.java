package com.anonymous.controller;

import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.service.IProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    IProductService productService;

    @PostMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<ProductResponse> insert(@RequestBody ProductRequest productRequest) {
        ProductResponse response = productService.insert(productRequest);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<ProductResponse> update(@PathVariable(value = "id") String oldProductId,
                                               @RequestBody ProductRequest productRequest) {
        ProductResponse response = productService.update(oldProductId, productRequest);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> delete(@RequestBody String[] productIds) {
        productService.delete(productIds);
        return ApiResponse.<String>builder()
                .message("Delete successfully ! - Xóa thành công !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<ProductResponse>> showAll() {
        List<ProductResponse> response = productService.getAll();
        return ApiResponse.<List<ProductResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-common-detail")
    public ApiResponse<ProductDetailResponse> getCommonDetail(
            @RequestParam(value = "productId") String productId,
            @RequestParam(value = "warehouseId") String warehouseId
    ) {
        ProductDetailResponse response = productService.getDetail(productId, warehouseId);
        return ApiResponse.<ProductDetailResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<ProductResponse> getDetail(@RequestParam(value = "productId") String productId) {
        ProductResponse response = productService.getDetail(productId);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

}
