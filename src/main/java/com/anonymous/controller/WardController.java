package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.WardResponse;
import com.anonymous.service.IWardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wards")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WardController {

    IWardService wardService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<WardResponse>> showAll() {
        List<WardResponse> response = wardService.getAll();
        return ApiResponse.<List<WardResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-by-district")
    public ApiResponse<List<WardResponse>> getAllByDistrict(@RequestParam(value = "districtId") String districtId) {
        List<WardResponse> response = wardService.getAllByDistrictId(districtId);
        return ApiResponse.<List<WardResponse>>builder()
                .result(response)
                .build();
    }

}
