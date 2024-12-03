package com.anonymous.controller;

import com.anonymous.dto.request.CustomerInsertRequest;
import com.anonymous.dto.request.CustomerUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.service.ICustomerService;
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
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CustomerResponse> insert(@RequestBody CustomerInsertRequest request) {
        CustomerResponse response = customerService.insert(request);
        return ApiResponse.<CustomerResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CustomerResponse> update(@PathVariable(value = "id") String oldEmployeeId,
                                                @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.update(oldEmployeeId, request);
        return ApiResponse.<CustomerResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<CustomerResponse>> showAll() {
        List<CustomerResponse> response = customerService.getAll();
        return ApiResponse.<List<CustomerResponse>>builder()
                .result(response)
                .build();
    }

}
