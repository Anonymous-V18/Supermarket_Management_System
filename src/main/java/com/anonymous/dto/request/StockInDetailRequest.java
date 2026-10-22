package com.anonymous.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class StockInDetailRequest {

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    Integer quantity;

    @NotNull(message = "Vui lòng nhập giá nhập")
    @DecimalMin(value = "0", message = "Giá nhập không được âm")
    BigDecimal inputPrice;

    // Becomes the product's retail price
    @NotNull(message = "Vui lòng nhập giá bán")
    @DecimalMin(value = "0", message = "Giá bán không được âm")
    BigDecimal salePrice;

    @Size(max = 50, message = "Số lô tối đa 50 ký tự")
    String lotNumber;

    // yyyy-MM-dd; must not be in the past for goods being received
    LocalDate expiryDate;

}
