package com.anonymous.controller;

import com.anonymous.dto.request.InvoiceInsertRequest;
import com.anonymous.dto.request.InvoiceUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.service.IInvoiceService;
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
    @PreAuthorize("hasAnyRole('ADMIN','SALESMAN','EMPLOYEE')")
    public ApiResponse<String> insert(@RequestBody InvoiceInsertRequest request) {
        invoiceService.insert(request);
        return ApiResponse.<String>builder()
                .message("Insert successfully !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALESMAN','EMPLOYEE')")
    public ApiResponse<String> update(@PathVariable(value = "id") String oldInvoiceId,
                                      @RequestBody InvoiceUpdateRequest request) {
        invoiceService.update(oldInvoiceId, request);
        return ApiResponse.<String>builder()
                .message("Update successfully !")
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('SALESMAN','EMPLOYEE')")
    public ApiResponse<String> delete(@RequestBody String[] invoiceIds) {
        invoiceService.delete(invoiceIds);
        return ApiResponse.<String>builder()
                .message("Delete successfully !")
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<InvoiceResponse>> showAll() {
        List<InvoiceResponse> response = invoiceService.getAll();
        return ApiResponse.<List<InvoiceResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<InvoiceResponse> getDetail(@RequestParam(value = "invoiceId") String invoiceId) {
        InvoiceResponse response = invoiceService.getById(invoiceId);
        return ApiResponse.<InvoiceResponse>builder()
                .result(response)
                .build();
    }

}
