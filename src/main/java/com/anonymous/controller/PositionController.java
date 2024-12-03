package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.PositionResponse;
import com.anonymous.service.IPositionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionController {

    IPositionService positionService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<PositionResponse>> showAll() {
        List<PositionResponse> response = positionService.getAll();
        return ApiResponse.<List<PositionResponse>>builder()
                .result(response)
                .build();
    }

}
