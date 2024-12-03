package com.anonymous.service;

import com.anonymous.dto.request.UserChangePasswordRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.UserResponse;
import com.anonymous.entity.User;

import java.util.Set;

public interface IUserService {

    User insert(UserInsertRequest request);

    void changePassword(UserChangePasswordRequest userChangePasswordRequest);

    void resetPassword(String userId);

    UserResponse update(String userId, User user);

    void changeRole(User user, Set<String> roleIds);
}
