package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.UserChangePasswordRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.service.IUserService;
import jakarta.validation.Valid;
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
    public ApiResponse<String> register(@RequestBody @Valid UserInsertRequest request) {
        userService.register(request);
        return ApiResponse.<String>builder()
                .message("Đăng ký thành công !")
                .build();
    }

    @PatchMapping("/update/{id}/reset-password")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<String> resetPassword(@PathVariable(value = "id") String userId) {
        userService.resetPassword(userId);
        return ApiResponse.<String>builder()
                .message("Đặt lại mật khẩu thành công !")
                .build();
    }

    @PatchMapping("/update/change-password")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<String> changePassword(@RequestBody @Valid UserChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.<String>builder()
                .message("Đổi mật khẩu thành công !")
                .build();
    }

}
