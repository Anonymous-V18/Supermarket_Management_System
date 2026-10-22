package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChecklistItemInsertRequest extends ChecklistItemRequest {

    // Stable key stored on completed checklists; can't be changed later
    @NotBlank(message = "Vui lòng nhập mã")
    @Pattern(regexp = "^[a-z0-9-]{2,50}$", message = "Mã gồm 2-50 ký tự chữ thường, số hoặc dấu gạch ngang")
    String code;

}
