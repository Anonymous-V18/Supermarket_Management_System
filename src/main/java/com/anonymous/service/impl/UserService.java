package com.anonymous.service.impl;

import com.anonymous.converter.IUserMapper;
import com.anonymous.dto.request.UserChangePasswordRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.UserResponse;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IUserRepository;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService implements IUserService {

    IUserRepository userRepository;
    IUserMapper userMapper;
    IAuthService authService;

    @Override
    public User insert(UserInsertRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .map(existingUser -> {
                    if (existingUser.getEmployee() != null) {
                        throw new AppException(ErrorCode.USER_EXISTED);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .username(request.getUsername())
                            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                            .build();
                    return userRepository.save(user);
                });
    }

    @Override
    public User register(UserInsertRequest request) {
        userRepository.findByUsername(request.getUsername())
                .ifPresent(_ -> {
                    throw new AppException(ErrorCode.USER_EXISTED);
                });

        User user = User.builder()
                .username(request.getUsername())
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    @Override
    public void changePassword(UserChangePasswordRequest userChangePasswordRequest) {
        log.info("Password changes are delegated to centralized Identity Service.");
    }

    @Override
    public void resetPassword(String userId) {
        log.info("Password resets are delegated to centralized Identity Service.");
    }

    @Override
    public UserResponse update(String userId, User user) {
        return null;
    }

    @Override
    public void changeRole(User user, Set<String> roleIds) {
        log.info("User role assignments are delegated to centralized Identity Service.");
    }
}
