package com.anonymous.controller;

import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.StockInResponse;
import com.anonymous.service.IStockInService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-ins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockInController {

    IStockInService stockInService;

    @PostMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> insert(@RequestBody StockInInsertRequest request) {
        stockInService.insert(request);
        return ApiResponse.<String>builder()
                .message("Insert successfully !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> update(@PathVariable(value = "id") String oldStockInId,
                                      @RequestBody StockInUpdateRequest request) {
        stockInService.update(oldStockInId, request);
        return ApiResponse.<String>builder()
                .message("Update successfully !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN','STOREKEEPER')")
    public ApiResponse<String> delete(@RequestBody String[] request) {
        stockInService.delete(request);
        return ApiResponse.<String>builder()
                .message("Delete successfully !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<StockInResponse>> showAll() {
        List<StockInResponse> response = stockInService.getAll();
        return ApiResponse.<List<StockInResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<StockInResponse> getDetail(@RequestParam(value = "stockInId") String stockInId) {
        StockInResponse response = stockInService.getById(stockInId);
        return ApiResponse.<StockInResponse>builder()
                .result(response)
                .build();
    }

}
