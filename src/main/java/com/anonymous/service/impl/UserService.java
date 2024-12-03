package com.anonymous.service.impl;

import com.anonymous.converter.IUserMapper;
import com.anonymous.dto.request.UserChangePasswordRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.UserResponse;
import com.anonymous.entity.Role;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IRoleRepository;
import com.anonymous.repository.IUserRepository;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService implements IUserService {

    IUserRepository userRepository;
    IUserMapper userMapper;
    IRoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    IAuthService authService;

    @Override
    public User insert(UserInsertRequest request) {
        userRepository.findByUsername(request.getUsername())
                .ifPresent(_ -> {
                    throw new AppException(ErrorCode.USER_EXISTED);
                });

        String password = passwordEncoder.encode(request.getPassword());
        request.setPassword(password);

        User user = userMapper.toEntity(request);

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
        if (roles.size() != request.getRoleIds().size()) {
            throw new AppException(ErrorCode.ROLE_NOT_EXIST);
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public void changePassword(UserChangePasswordRequest userChangePasswordRequest) {
        String username = authService.getClaimsToken().get("username").toString();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        String oldPassword = userChangePasswordRequest.getOldPassword();
        boolean isPasswordValid = passwordEncoder.matches(oldPassword, user.getPassword());
        if (!isPasswordValid) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        String newPassword = passwordEncoder.encode(userChangePasswordRequest.getNewPassword());
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        String newPassword = passwordEncoder.encode("12345678");
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Override
    public UserResponse update(String userId, User user) {
        return null;
    }

    @Override
    public void changeRole(User user, Set<String> roleIds) {
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()) {
            throw new AppException(ErrorCode.ROLE_NOT_EXIST);
        }
        user.setRoles(roles);
        userRepository.save(user);
    }
}
