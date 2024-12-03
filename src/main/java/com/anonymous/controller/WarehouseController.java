package com.anonymous.controller;

import com.anonymous.dto.request.WarehouseInsertRequest;
import com.anonymous.dto.request.WarehouseUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.WarehouseResponse;
import com.anonymous.dto.response.WarehouseToSelectionResponse;
import com.anonymous.service.IWarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseController {

    IWarehouseService warehouseService;

    @PostMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<WarehouseResponse> insert(@RequestBody WarehouseInsertRequest request) {
        WarehouseResponse response = warehouseService.insert(request);
        return ApiResponse.<WarehouseResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<WarehouseResponse> update(@PathVariable(value = "id") String oldWarehouseId,
                                                 @RequestBody WarehouseUpdateRequest request) {
        WarehouseResponse response = warehouseService.update(oldWarehouseId, request);
        return ApiResponse.<WarehouseResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<WarehouseResponse>> showAll() {
        List<WarehouseResponse> response = warehouseService.getAll();
        return ApiResponse.<List<WarehouseResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-to-selection")
    public ApiResponse<List<WarehouseToSelectionResponse>> getAllToSelection() {
        List<WarehouseToSelectionResponse> response = warehouseService.getAllToSelection();
        return ApiResponse.<List<WarehouseToSelectionResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("get-detail")
    public ApiResponse<WarehouseResponse> getDetail(@RequestParam(value = "warehouseId") String warehouseId) {
        WarehouseResponse response = warehouseService.getById(warehouseId);
        return ApiResponse.<WarehouseResponse>builder()
                .result(response)
                .build();
    }
}
