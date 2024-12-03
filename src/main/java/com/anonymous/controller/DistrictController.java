package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.DistrictResponse;
import com.anonymous.service.IDistrictService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/districts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistrictController {

    IDistrictService districtService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<DistrictResponse>> showAll() {
        List<DistrictResponse> response = districtService.getAll();
        return ApiResponse.<List<DistrictResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-by-city")
    public ApiResponse<List<DistrictResponse>> getAllByCity(@RequestParam(value = "cityId") String cityId) {
        List<DistrictResponse> response = districtService.getAllByCityId(cityId);
        return ApiResponse.<List<DistrictResponse>>builder()
                .result(response)
                .build();
    }

}
