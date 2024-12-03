package com.anonymous.controller;

import com.anonymous.dto.request.UserChangePasswordRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/users")
public class UserController {

    IUserService userService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody UserInsertRequest request) {
        userService.insert(request);
        return ApiResponse.<String>builder()
                .message("Register successfully !")
                .build();
    }

    @PatchMapping("/update/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> resetPassword(@PathVariable(value = "id") String userId) {
        userService.resetPassword(userId);
        return ApiResponse.<String>builder()
                .message("Reset password successfully !")
                .build();
    }

    @PatchMapping("/update/change-password")
    public ApiResponse<String> changePassword(@RequestBody UserChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.<String>builder()
                .message("Change password successfully !")
                .build();
    }

}
