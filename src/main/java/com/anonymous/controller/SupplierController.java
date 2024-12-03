package com.anonymous.controller;

import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;
import com.anonymous.service.ISupplierService;
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
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<SupplierResponse> insert(@RequestBody SupplierInsertRequest request) {
        SupplierResponse response = supplierService.insert(request);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<SupplierResponse> update(@PathVariable(value = "id") String oldSupplierId,
                                                @RequestBody SupplierUpdateRequest request) {
        SupplierResponse response = supplierService.update(oldSupplierId, request);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<SupplierResponse>> showAll() {
        List<SupplierResponse> response = supplierService.getAll();
        return ApiResponse.<List<SupplierResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-to-selection")
    public ApiResponse<List<SupplierToSelectionResponse>> getAllToSelection() {
        List<SupplierToSelectionResponse> response = supplierService.getAllToSelection();
        return ApiResponse.<List<SupplierToSelectionResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<SupplierResponse> getDetail(@RequestParam(value = "supplierId") String supplierId) {
        SupplierResponse response = supplierService.getById(supplierId);
        return ApiResponse.<SupplierResponse>builder()
                .result(response)
                .build();
    }

}
