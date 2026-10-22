package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockOutResponse;
import com.anonymous.service.IStockOutService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
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
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockOutResponse> insert(@RequestBody @Valid StockOutInsertRequest request) {
        StockOutResponse response = stockOutService.insert(request);
        return ApiResponse.<StockOutResponse>builder()
                .result(response)
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockOutResponse> update(@PathVariable(value = "id") String oldStockOutId,
                                      @RequestBody @Valid StockOutUpdateRequest request) {
        StockOutResponse response = stockOutService.update(oldStockOutId, request);
        return ApiResponse.<StockOutResponse>builder()
                .result(response)
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<String> delete(@RequestBody String[] stockOutIds) {
        stockOutService.delete(stockOutIds);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<StockOutResponse>> showAll() {
        List<StockOutResponse> response = stockOutService.getAll();
        return ApiResponse.<List<StockOutResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<StockOutResponse> getDetail(@RequestParam(value = "stockOutId") String stockOutId) {
        StockOutResponse response = stockOutService.getById(stockOutId);
        return ApiResponse.<StockOutResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<PageResponse<StockOutResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<StockOutResponse> response = stockOutService.search(keyword, status, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<StockOutResponse>>builder()
                .result(response)
                .build();
    }

}
