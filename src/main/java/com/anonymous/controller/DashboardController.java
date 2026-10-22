package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.ShiftChecklistRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.DashboardSummaryResponse;
import com.anonymous.dto.response.ShiftChecklistResponse;
import com.anonymous.service.IDashboardService;
import com.anonymous.service.IShiftChecklistService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    IShiftChecklistService shiftChecklistService;
    IDashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<DashboardSummaryResponse> getSummary(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift
    ) {
        DashboardSummaryResponse response = dashboardService.getSummary(date, shift);
        return ApiResponse.<DashboardSummaryResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/shift-checklist")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ShiftChecklistResponse> getShiftChecklist(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift
    ) {
        ShiftChecklistResponse response = shiftChecklistService.getChecklist(date, shift);
        return ApiResponse.<ShiftChecklistResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/shift-checklist")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ShiftChecklistResponse> saveOrUpdateShiftChecklist(
            @RequestBody @Valid ShiftChecklistRequest request
    ) {
        ShiftChecklistResponse response = shiftChecklistService.saveOrUpdateChecklist(request);
        return ApiResponse.<ShiftChecklistResponse>builder()
                .result(response)
                .build();
    }

}
