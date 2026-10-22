package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class EmployeeRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    String name;

    @NotBlank(message = "Vui lòng chọn giới tính")
    @Pattern(regexp = "Nam|Nữ|Khác", message = "Giới tính chỉ nhận Nam, Nữ hoặc Khác")
    String gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull(message = "Vui lòng nhập ngày sinh")
    @Past(message = "Ngày sinh phải ở quá khứ")
    LocalDate dob;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải gồm 10-11 chữ số, bắt đầu bằng 0")
    String phoneNumber;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Vui lòng chọn vị trí làm việc")
    String positionId;

}
