package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.service.ICustomerService;
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
@RequestMapping("/customers")
public class CustomerController {

    ICustomerService customerService;

    @PostMapping("/insert")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<CustomerResponse> insert(@RequestBody @Valid CustomerInsertRequest request) {
        CustomerResponse response = customerService.insert(request);
        return ApiResponse.<CustomerResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<CustomerResponse> update(@PathVariable(value = "id") String oldEmployeeId,
                                                @RequestBody @Valid CustomerUpdateRequest request) {
        CustomerResponse response = customerService.update(oldEmployeeId, request);
        return ApiResponse.<CustomerResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<List<CustomerResponse>> showAll() {
        List<CustomerResponse> response = customerService.getAll();
        return ApiResponse.<List<CustomerResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.SALES)
    public ApiResponse<PageResponse<CustomerResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResponse<CustomerResponse> response = customerService.search(keyword, PageUtil.newestFirst(page, size));
        return ApiResponse.<PageResponse<CustomerResponse>>builder()
                .result(response)
                .build();
    }

}
