package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.StatusProductResponse;
import com.anonymous.service.IStatusProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/status-products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatusProductController {

    IStatusProductService statusProductService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<StatusProductResponse>> showAll() {
        List<StatusProductResponse> response = statusProductService.getAll();
        return ApiResponse.<List<StatusProductResponse>>builder()
                .result(response)
                .build();
    }

}
