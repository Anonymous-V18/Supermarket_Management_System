package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.EmployeeInsertRequest;
import com.anonymous.dto.request.EmployeeUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.service.IEmployeeService;
import com.anonymous.utils.PageUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/employees")
public class EmployeeController {

    IEmployeeService employeeService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<EmployeeResponse> insert(@RequestBody @Valid EmployeeInsertRequest request) {
        EmployeeResponse response = employeeService.insert(request);
        return ApiResponse.<EmployeeResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<EmployeeResponse> update(@RequestBody @Valid EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.update(request);
        return ApiResponse.<EmployeeResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<EmployeeResponse> update(@PathVariable(value = "id") String oldEmployeeId,
                                                @RequestBody @Valid EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.update(oldEmployeeId, request);
        return ApiResponse.<EmployeeResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<List<EmployeeResponse>> showAll() {
        List<EmployeeResponse> response = employeeService.getAll();
        return ApiResponse.<List<EmployeeResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-detail")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<EmployeeResponse> getDetail(@RequestParam(value = "employeeId") String employeeId) {
        EmployeeResponse response = employeeService.getById(employeeId);
        return ApiResponse.<EmployeeResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-my-info")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<EmployeeResponse> getMyInfo() {
        EmployeeResponse response = employeeService.getMyInfo();
        return ApiResponse.<EmployeeResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/get-all-employees")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<List<EmployeeResponse>> searchCriteria(@RequestParam(value = "page") int page,
                                                              @RequestParam(value = "limit") int limit,
                                                              @RequestParam(value = "sortBy") String sortBy,
                                                              @RequestParam(value = "search", required = false) String... search) {
        List<EmployeeResponse> response = employeeService.getBySearchCriteria(page, limit, sortBy, search);
        return ApiResponse.<List<EmployeeResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<PageResponse<EmployeeResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<EmployeeResponse> response = employeeService.search(keyword, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<EmployeeResponse>>builder()
                .result(response)
                .build();
    }

}
