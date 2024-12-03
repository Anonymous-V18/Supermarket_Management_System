package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.StatusInvoiceResponse;
import com.anonymous.service.IStatusInvoiceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/status-invoices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatusInvoiceController {

    IStatusInvoiceService statusInvoiceService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<StatusInvoiceResponse>> showAll() {
        List<StatusInvoiceResponse> response = statusInvoiceService.getAll();
        return ApiResponse.<List<StatusInvoiceResponse>>builder()
                .result(response)
                .build();
    }

}
