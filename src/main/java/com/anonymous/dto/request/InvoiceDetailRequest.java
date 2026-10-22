package com.anonymous.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class InvoiceDetailRequest {

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    Integer quantity;

    @DecimalMin(value = "0", message = "Chiết khấu không được âm")
    @DecimalMax(value = "100", message = "Chiết khấu tối đa 100%")
    BigDecimal discountPercent = BigDecimal.ZERO;

}
