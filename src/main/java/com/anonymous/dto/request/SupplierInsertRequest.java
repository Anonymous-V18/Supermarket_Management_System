package com.anonymous.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupplierInsertRequest extends SupplierRequest {

    @NotNull(message = "Vui lòng nhập địa chỉ")
    @Valid
    AddressInsertRequest newAddress;

}
