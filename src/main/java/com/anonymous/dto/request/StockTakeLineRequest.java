package com.anonymous.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTakeLineRequest {

    @NotBlank(message = "Vui lòng chọn sản phẩm")
    String productId;

    @NotNull(message = "Vui lòng nhập số lượng đếm được")
    @Min(value = 0, message = "Số lượng đếm được không được âm")
    Integer countedQuantity;

}
