package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AddressRequest {

    @NotBlank(message = "Vui lòng chọn tỉnh / thành phố")
    String cityId;

    @NotBlank(message = "Vui lòng chọn quận / huyện")
    String districtId;

    @NotBlank(message = "Vui lòng chọn phường / xã")
    String wardId;

    @NotBlank(message = "Vui lòng nhập số nhà, tên đường")
    @Size(min = 3, max = 255, message = "Số nhà, tên đường từ 3 đến 255 ký tự")
    String street;

}
