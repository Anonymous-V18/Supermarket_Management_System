package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;
import com.anonymous.service.ISupplierService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierController {

    ISupplierService supplierService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<SupplierResponse> insert(@RequestBody @Valid SupplierInsertRequest request) {
        SupplierResponse response = supplierService.insert(request);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<SupplierResponse> update(@PathVariable(value = "id") String oldSupplierId,
                                                @RequestBody @Valid SupplierUpdateRequest request) {
        SupplierResponse response = supplierService.update(oldSupplierId, request);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<SupplierResponse>> showAll() {
        List<SupplierResponse> response = supplierService.getAll();
        return ApiResponse.<List<SupplierResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-to-selection")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<SupplierToSelectionResponse>> getAllToSelection() {
        List<SupplierToSelectionResponse> response = supplierService.getAllToSelection();
        return ApiResponse.<List<SupplierToSelectionResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<SupplierResponse> getDetail(@RequestParam(value = "supplierId") String supplierId) {
        SupplierResponse response = supplierService.getById(supplierId);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<PageResponse<SupplierResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<SupplierResponse> response = supplierService.search(keyword, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<SupplierResponse>>builder()
                .result(response)
                .build();
    }

}
