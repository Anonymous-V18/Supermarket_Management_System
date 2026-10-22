package com.anonymous.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeInsertRequest extends EmployeeRequest {

    @NotEmpty(message = "Vui lòng chọn ít nhất một quyền")
    Set<String> roleIds = new HashSet<>();

    @NotNull(message = "Vui lòng nhập địa chỉ")
    @Valid
    AddressInsertRequest newAddress;

}
