package com.anonymous.client;

import com.anonymous.dto.identity.IdentityApiResponse;
import com.anonymous.dto.identity.IdentityAuthResult;
import com.anonymous.dto.identity.IdentityIntrospectResult;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class IdentityClient {

    private final RestClient restClient;
    private final String clientId;

    public IdentityClient(
            @Value("${identity.service.base-url:http://localhost:8080}") String baseUrl,
            @Value("${identity.service.client-id:supermarket-app}") String clientId
    ) {
        this.clientId = clientId;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public IdentityAuthResult login(String username, String password) {
        log.info("Authenticating user [{}] with Identity Service for client [{}]", username, clientId);
        try {
            IdentityApiResponse<IdentityAuthResult> response = restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", username,
                            "password", password,
                            "clientId", clientId
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<IdentityApiResponse<IdentityAuthResult>>() {});

            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
            throw new AppException(ErrorCode.LOGIN_FAILED);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to authenticate with Identity Service: {}", e.getMessage());
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }
    }

    public IdentityIntrospectResult introspect(String token) {
        try {
            IdentityApiResponse<IdentityIntrospectResult> response = restClient.post()
                    .uri("/api/v1/auth/introspect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("token", token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<IdentityApiResponse<IdentityIntrospectResult>>() {});

            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
            return IdentityIntrospectResult.builder().isValid(false).build();
        } catch (Exception e) {
            log.warn("Token introspection failed with Identity Service: {}", e.getMessage());
            return IdentityIntrospectResult.builder().isValid(false).build();
        }
    }

    public void logout(String token) {
        try {
            restClient.post()
                    .uri("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("accessToken", token))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notified Identity Service to revoke token during logout.");
        } catch (Exception e) {
            log.warn("Failed to notify Identity Service of logout: {}", e.getMessage());
        }
    }

    public IdentityAuthResult refreshToken(String refreshToken) {
        try {
            IdentityApiResponse<IdentityAuthResult> response = restClient.post()
                    .uri("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<IdentityApiResponse<IdentityAuthResult>>() {});

            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
            throw new AppException(ErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("Failed to refresh token with Identity Service: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    public java.util.List<com.anonymous.dto.response.RoleResponse> getRoles() {
        try {
            IdentityApiResponse<java.util.List<com.anonymous.dto.response.RoleResponse>> response = restClient.get()
                    .uri("/api/v1/auth/roles?clientId=" + clientId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<IdentityApiResponse<java.util.List<com.anonymous.dto.response.RoleResponse>>>() {});

            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
            return java.util.Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch roles from Identity Service: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
