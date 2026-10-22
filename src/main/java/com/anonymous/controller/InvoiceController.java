package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.service.IInvoiceService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceController {

    IInvoiceService invoiceService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<InvoiceResponse> insert(@RequestBody @Valid InvoiceInsertRequest request) {
        InvoiceResponse response = invoiceService.insert(request);
        return ApiResponse.<InvoiceResponse>builder()
                .result(response)
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<InvoiceResponse> update(@PathVariable(value = "id") String oldInvoiceId,
                                      @RequestBody @Valid InvoiceUpdateRequest request) {
        InvoiceResponse response = invoiceService.update(oldInvoiceId, request);
        return ApiResponse.<InvoiceResponse>builder()
                .result(response)
                .message("Cập nhật thành công !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<String> delete(@RequestBody String[] invoiceIds) {
        invoiceService.delete(invoiceIds);
        return ApiResponse.<String>builder()
                .message("Xóa thành công !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<List<InvoiceResponse>> showAll() {
        List<InvoiceResponse> response = invoiceService.getAll();
        return ApiResponse.<List<InvoiceResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<InvoiceResponse> getDetail(@RequestParam(value = "invoiceId") String invoiceId) {
        InvoiceResponse response = invoiceService.getById(invoiceId);
        return ApiResponse.<InvoiceResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.SALES_READ)
    public ApiResponse<PageResponse<InvoiceResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<InvoiceResponse> response = invoiceService.search(keyword, status, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<InvoiceResponse>>builder()
                .result(response)
                .build();
    }

}
