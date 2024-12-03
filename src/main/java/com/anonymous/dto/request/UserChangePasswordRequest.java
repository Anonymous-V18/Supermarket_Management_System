package com.anonymous.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserChangePasswordRequest {

    String oldPassword;
    String newPassword;
    
}
