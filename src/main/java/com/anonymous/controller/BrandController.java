package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.BrandResponse;
import com.anonymous.service.IBrandService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandController {

    IBrandService brandService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<BrandResponse>> showAll() {
        List<BrandResponse> response = brandService.getAll();
        return ApiResponse.<List<BrandResponse>>builder()
                .result(response)
                .build();
    }

}
