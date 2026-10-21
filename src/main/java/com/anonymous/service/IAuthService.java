package com.anonymous.service;

import com.anonymous.dto.request.LoginRequest;
import com.anonymous.dto.request.LogoutRequest;
import com.anonymous.dto.request.RefreshTokenRequest;
import com.anonymous.dto.response.AuthResponse;
import com.anonymous.dto.response.RefreshTokenResponse;

import java.util.Map;

public interface IAuthService {

    AuthResponse login(LoginRequest request);

    boolean introspectToken(String token);

    void logout(LogoutRequest logoutRequest);

    RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    Map<String, Object> getClaimsToken();
}
