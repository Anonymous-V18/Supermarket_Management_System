package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.CityResponse;
import com.anonymous.service.ICityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CityController {

    ICityService cityService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<CityResponse>> showAll() {
        List<CityResponse> response = cityService.getAll();
        return ApiResponse.<List<CityResponse>>builder()
                .result(response)
                .build();
    }

}
