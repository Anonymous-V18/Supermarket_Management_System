package com.anonymous.service.impl;

import com.anonymous.converter.ICustomerMapper;
import com.anonymous.converter.IEmployeeMapper;
import com.anonymous.dto.request.LoginRequest;
import com.anonymous.dto.request.LogoutRequest;
import com.anonymous.dto.request.RefreshTokenRequest;
import com.anonymous.dto.response.AuthResponse;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.dto.response.RefreshTokenResponse;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IUserRepository;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IInvalidatedTokenService;
import com.anonymous.utils.JwtUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService implements IAuthService {

    IUserRepository userRepository;
    IInvalidatedTokenService invalidatedTokenService;
    IEmployeeMapper employeeMapper;
    ICustomerMapper customerMapper;
    JwtUtil jwtUtil;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isPasswordValid) {
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtUtil.generateToken(user);
        EmployeeResponse employeeResponse = user.getEmployee() == null
                ? null : employeeMapper.toDTO(user.getEmployee());
        CustomerResponse customerResponse = user.getCustomer() == null
                ? null : customerMapper.toDTO(user.getCustomer());
        return new AuthResponse(token, employeeResponse, customerResponse);
    }

    @Override
    public boolean introspectToken(String token) {
        boolean isAuthenticated = true;
        try {
            jwtUtil.verifyToken(token, false);
        } catch (JOSEException | ParseException | AppException e) {
            isAuthenticated = false;
        }
        return isAuthenticated;
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        String token = logoutRequest.getToken();
        try {
            SignedJWT signedJWT = jwtUtil.verifyToken(token, true);
            invalidatedTokenService.insert(signedJWT);
        } catch (ParseException | JOSEException | AppException e) {
            log.info("Logout: Token invalid !");
        }
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String newToken;
        try {
            String oldToken = refreshTokenRequest.getToken();
            SignedJWT signedJWT = jwtUtil.verifyToken(oldToken, true);
            invalidatedTokenService.insert(signedJWT);

            String username = signedJWT.getJWTClaimsSet().getStringClaim("username");
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            newToken = jwtUtil.generateToken(user);
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        return RefreshTokenResponse.builder()
                .token(newToken)
                .build();
    }

    @Override
    public Map<String, Object> getClaimsToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> currentClaims = new HashMap<>();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            currentClaims = ((JwtAuthenticationToken) authentication).getToken().getClaims();
        }
        return currentClaims;
    }

}
