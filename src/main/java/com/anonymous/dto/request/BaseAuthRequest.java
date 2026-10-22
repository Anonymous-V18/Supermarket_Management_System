package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class BaseAuthRequest {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    String password;

}
