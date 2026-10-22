package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.StockTransferInsertRequest;
import com.anonymous.dto.request.StockTransferRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTransferResponse;
import com.anonymous.service.IStockTransferService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock-transfers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockTransferController {

    IStockTransferService stockTransferService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockTransferResponse> insert(@RequestBody @Valid StockTransferInsertRequest request) {
        return ApiResponse.<StockTransferResponse>builder()
                .result(stockTransferService.insert(request))
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<StockTransferResponse> update(@PathVariable(value = "id") String id,
                                                 @RequestBody @Valid StockTransferRequest request) {
        return ApiResponse.<StockTransferResponse>builder()
                .result(stockTransferService.update(id, request))
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.STOCK)
    public ApiResponse<String> delete(@RequestBody String[] ids) {
        stockTransferService.delete(ids);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<StockTransferResponse> getDetail(@RequestParam(value = "id") String id) {
        return ApiResponse.<StockTransferResponse>builder()
                .result(stockTransferService.getById(id))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.STOCK_READ)
    public ApiResponse<PageResponse<StockTransferResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<StockTransferResponse>>builder()
                .result(stockTransferService.search(keyword, status, PageUtil.newestFirst(page, size)))
                .build();
    }

}
