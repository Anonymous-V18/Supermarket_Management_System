package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.StockTakeInsertRequest;
import com.anonymous.dto.request.StockTakeRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTakeResponse;
import com.anonymous.service.IStockTakeService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock-takes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockTakeController {

    IStockTakeService stockTakeService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockTakeResponse> insert(@RequestBody @Valid StockTakeInsertRequest request) {
        return ApiResponse.<StockTakeResponse>builder()
                .result(stockTakeService.insert(request))
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockTakeResponse> update(@PathVariable(value = "id") String id,
                                                 @RequestBody @Valid StockTakeRequest request) {
        return ApiResponse.<StockTakeResponse>builder()
                .result(stockTakeService.update(id, request))
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<String> delete(@RequestBody String[] ids) {
        stockTakeService.delete(ids);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<StockTakeResponse> getDetail(@RequestParam(value = "id") String id) {
        return ApiResponse.<StockTakeResponse>builder()
                .result(stockTakeService.getById(id))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<PageResponse<StockTakeResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<StockTakeResponse>>builder()
                .result(stockTakeService.search(keyword, status, PageUtil.newestFirst(page, size)))
                .build();
    }

}
