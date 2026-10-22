package com.anonymous.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChecklistItemRequest {

    @NotBlank(message = "Vui lòng nhập nội dung công việc")
    @Size(max = 255, message = "Nội dung tối đa 255 ký tự")
    String label;

    @NotBlank(message = "Vui lòng chọn nhóm")
    @Pattern(regexp = "START|END", message = "Nhóm phải là đầu ca hoặc cuối ca")
    String groupCode;

    @NotNull(message = "Vui lòng nhập thứ tự")
    @Min(value = 0, message = "Thứ tự không được âm")
    Integer sortOrder;

    @NotNull(message = "Vui lòng chọn trạng thái")
    Boolean isActive;

}
