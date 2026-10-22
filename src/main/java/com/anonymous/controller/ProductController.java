package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.service.IProductService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
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
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<ProductResponse> insert(@RequestBody @Valid ProductRequest productRequest) {
        ProductResponse response = productService.insert(productRequest);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<ProductResponse> update(@PathVariable(value = "id") String oldProductId,
                                               @RequestBody @Valid ProductRequest productRequest) {
        ProductResponse response = productService.update(oldProductId, productRequest);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<String> delete(@RequestBody String[] productIds) {
        productService.delete(productIds);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<List<ProductResponse>> showAll() {
        List<ProductResponse> response = productService.getAll();
        return ApiResponse.<List<ProductResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-common-detail")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ProductDetailResponse> getCommonDetail(
            @RequestParam(value = "productId") String productId,
            @RequestParam(value = "warehouseId") String warehouseId
    ) {
        ProductDetailResponse response = productService.getDetail(productId, warehouseId);
        return ApiResponse.<ProductDetailResponse>builder()
                .result(response)
                .build();
    }

    // Barcode scanner on the sales screen: price and stock of the scanned product in that warehouse
    @GetMapping("/get-by-barcode")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ProductDetailResponse> getByBarcode(
            @RequestParam(value = "barcode") String barcode,
            @RequestParam(value = "warehouseId") String warehouseId
    ) {
        ProductDetailResponse response = productService.getByBarcode(barcode, warehouseId);
        return ApiResponse.<ProductDetailResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ProductResponse> getDetail(@RequestParam(value = "productId") String productId) {
        ProductResponse response = productService.getDetail(productId);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<ProductResponse> response = productService.search(keyword, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .result(response)
                .build();
    }

}
