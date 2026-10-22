package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public abstract class CustomerRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    String name;

    @Pattern(regexp = "Nam|Nữ|Khác", message = "Giới tính chỉ nhận Nam, Nữ hoặc Khác")
    String gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Past(message = "Ngày sinh phải ở quá khứ")
    LocalDate dob;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải gồm 10-11 chữ số, bắt đầu bằng 0")
    String phoneNumber;

    @Email(message = "Email không hợp lệ")
    String email;

}
