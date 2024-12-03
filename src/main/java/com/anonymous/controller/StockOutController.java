package com.anonymous.controller;

import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.StockOutResponse;
import com.anonymous.service.IStockOutService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-outs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockOutController {

    IStockOutService stockOutService;

    @PostMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> insert(@RequestBody StockOutInsertRequest request) {
        stockOutService.insert(request);
        return ApiResponse.<String>builder()
                .message("Insert successfully !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> update(@PathVariable(value = "id") String oldStockOutId,
                                      @RequestBody StockOutUpdateRequest request) {
        stockOutService.update(oldStockOutId, request);
        return ApiResponse.<String>builder()
                .message("Update successfully !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> delete(@RequestBody String[] stockOutIds) {
        stockOutService.delete(stockOutIds);
        return ApiResponse.<String>builder()
                .message("Delete successfully !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<StockOutResponse>> showAll() {
        List<StockOutResponse> response = stockOutService.getAll();
        return ApiResponse.<List<StockOutResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<StockOutResponse> getDetail(@RequestParam(value = "stockOutId") String stockOutId) {
        StockOutResponse response = stockOutService.getById(stockOutId);
        return ApiResponse.<StockOutResponse>builder()
                .result(response)
                .build();
    }
}
