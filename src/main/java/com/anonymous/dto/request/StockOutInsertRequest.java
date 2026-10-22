package com.anonymous.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOutInsertRequest extends StockOutRequest {

    @NotBlank(message = "Vui lòng chọn kho")
    String warehouseId;

    @NotEmpty(message = "Phiếu xuất phải có ít nhất một sản phẩm")
    @Valid
    Set<StockOutDetailInsertRequest> stockOutDetailInsertRequests = new HashSet<>();

}
