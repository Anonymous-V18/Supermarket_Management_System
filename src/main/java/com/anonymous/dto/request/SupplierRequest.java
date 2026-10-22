package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SupplierRequest {

    @NotBlank(message = "Vui lòng nhập tên nhà cung cấp")
    @Size(max = 255, message = "Tên nhà cung cấp tối đa 255 ký tự")
    String name;

    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải gồm 10-11 chữ số, bắt đầu bằng 0")
    String phoneNumber;

    @Size(max = 255, message = "Thông tin thêm tối đa 255 ký tự")
    String moreInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
    @PastOrPresent(message = "Ngày liên hệ không được ở tương lai")
    Instant contactDate;

}
