package com.anonymous.controller;

import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.RoleResponse;
import com.anonymous.service.IRoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/roles")
public class RoleController {

    IRoleService roleService;

    @GetMapping("/show-all-no-param")
    public ApiResponse<List<RoleResponse>> showAll() {
        List<RoleResponse> response = roleService.findAll();
        return ApiResponse.<List<RoleResponse>>builder()
                .result(response)
                .build();
    }

}
