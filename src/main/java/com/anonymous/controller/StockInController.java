package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockInResponse;
import com.anonymous.service.IStockInService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
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
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockInResponse> insert(@RequestBody @Valid StockInInsertRequest request) {
        StockInResponse response = stockInService.insert(request);
        return ApiResponse.<StockInResponse>builder()
                .result(response)
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockInResponse> update(@PathVariable(value = "id") String oldStockInId,
                                      @RequestBody @Valid StockInUpdateRequest request) {
        StockInResponse response = stockInService.update(oldStockInId, request);
        return ApiResponse.<StockInResponse>builder()
                .result(response)
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<String> delete(@RequestBody String[] request) {
        stockInService.delete(request);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<List<StockInResponse>> showAll() {
        List<StockInResponse> response = stockInService.getAll();
        return ApiResponse.<List<StockInResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<StockInResponse> getDetail(@RequestParam(value = "stockInId") String stockInId) {
        StockInResponse response = stockInService.getById(stockInId);
        return ApiResponse.<StockInResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<PageResponse<StockInResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<StockInResponse> response = stockInService.search(keyword, status, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<StockInResponse>>builder()
                .result(response)
                .build();
    }

}
