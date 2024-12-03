package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.UnitResponse;
import com.anonymous.service.IUnitService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UnitController {

    IUnitService unitService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<UnitResponse>> showAll() {
        List<UnitResponse> response = unitService.getAll();
        return ApiResponse.<List<UnitResponse>>builder()
                .result(response)
                .build();
    }

}
