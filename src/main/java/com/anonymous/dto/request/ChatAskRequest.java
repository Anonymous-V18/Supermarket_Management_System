package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatAskRequest {

    // Empty: start a new conversation
    String conversationId;

    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 1000, message = "Câu hỏi tối đa 1000 ký tự")
    String question;

    // all: documents and data, docs: documents only, data: live data only
    @Pattern(regexp = "^(all|docs|data)$", message = "Phạm vi không hợp lệ")
    String scope = "all";

}
