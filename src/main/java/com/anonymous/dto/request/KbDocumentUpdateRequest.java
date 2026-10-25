package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KbDocumentUpdateRequest {

    @NotBlank(message = "Vui lòng nhập tên tài liệu")
    @Size(max = 255, message = "Tên tài liệu tối đa 255 ký tự")
    String title;

    // Role codes allowed to read the document through the chatbot
    @NotEmpty(message = "Chọn ít nhất một vai trò được xem tài liệu")
    Set<String> roleCodes;

}
