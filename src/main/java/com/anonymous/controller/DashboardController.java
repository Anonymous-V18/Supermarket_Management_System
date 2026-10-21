package com.anonymous.controller;

import com.anonymous.dto.request.ShiftChecklistRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.ShiftChecklistResponse;
import com.anonymous.service.IShiftChecklistService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    IShiftChecklistService shiftChecklistService;

    @GetMapping("/shift-checklist")
    public ApiResponse<ShiftChecklistResponse> getShiftChecklist(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "username", required = false) String username
    ) {
        ShiftChecklistResponse response = shiftChecklistService.getChecklist(date, shift, username);
        return ApiResponse.<ShiftChecklistResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/shift-checklist")
    public ApiResponse<ShiftChecklistResponse> saveOrUpdateShiftChecklist(
            @RequestBody ShiftChecklistRequest request,
            @RequestParam(value = "username", required = false) String username
    ) {
        ShiftChecklistResponse response = shiftChecklistService.saveOrUpdateChecklist(request, username);
        return ApiResponse.<ShiftChecklistResponse>builder()
                .result(response)
                .build();
    }

}
