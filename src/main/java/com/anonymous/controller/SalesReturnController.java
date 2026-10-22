package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.SalesReturnInsertRequest;
import com.anonymous.dto.request.SalesReturnRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.SalesReturnResponse;
import com.anonymous.dto.response.ReturnableLineResponse;
import com.anonymous.service.ISalesReturnService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales-returns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesReturnController {

    ISalesReturnService salesReturnService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<SalesReturnResponse> insert(@RequestBody @Valid SalesReturnInsertRequest request) {
        return ApiResponse.<SalesReturnResponse>builder()
                .result(salesReturnService.insert(request))
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<SalesReturnResponse> update(@PathVariable(value = "id") String id,
                                                 @RequestBody @Valid SalesReturnRequest request) {
        return ApiResponse.<SalesReturnResponse>builder()
                .result(salesReturnService.update(id, request))
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<String> delete(@RequestBody String[] ids) {
        salesReturnService.delete(ids);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<SalesReturnResponse> getDetail(@RequestParam(value = "id") String id) {
        return ApiResponse.<SalesReturnResponse>builder()
                .result(salesReturnService.getById(id))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<PageResponse<SalesReturnResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<SalesReturnResponse>>builder()
                .result(salesReturnService.search(keyword, status, PageUtil.newestFirst(page, size)))
                .build();
    }

    // Lines of an invoice with what is still returnable, to prefill the return form
    @GetMapping("/returnable-lines")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<List<ReturnableLineResponse>> getReturnableLines(@RequestParam(value = "invoiceId") String invoiceId) {
        return ApiResponse.<List<ReturnableLineResponse>>builder()
                .result(salesReturnService.getReturnableLines(invoiceId))
                .build();
    }

}
