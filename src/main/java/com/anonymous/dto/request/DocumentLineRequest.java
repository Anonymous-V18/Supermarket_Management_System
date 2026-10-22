package com.anonymous.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// A product and a quantity: lines of sales returns and stock transfers
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentLineRequest {

    @NotBlank(message = "Vui lòng chọn sản phẩm")
    String productId;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    Integer quantity;

}
