package com.anonymous.controller;

import com.anonymous.dto.request.IntrospectTokenRequest;
import com.anonymous.dto.request.LoginRequest;
import com.anonymous.dto.request.LogoutRequest;
import com.anonymous.dto.request.RefreshTokenRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.AuthResponse;
import com.anonymous.dto.response.IntrospectTokenResponse;
import com.anonymous.dto.response.RefreshTokenResponse;
import com.anonymous.service.IAuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/auth")
public class AuthController {

    IAuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.<String>builder()
                .message("Logout successfully ! - Đăng xuất thành công !")
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ApiResponse.<RefreshTokenResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/introspect-token")
    public ApiResponse<IntrospectTokenResponse> introspectToken(@RequestBody IntrospectTokenRequest request) {
        String token = request.getToken();
        boolean isAuthenticated = authService.introspectToken(token);
        IntrospectTokenResponse response = new IntrospectTokenResponse(isAuthenticated);
        return ApiResponse.<IntrospectTokenResponse>builder()
                .result(response)
                .build();
    }

}
