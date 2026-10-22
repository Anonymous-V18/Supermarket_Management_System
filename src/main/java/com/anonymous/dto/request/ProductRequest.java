package com.anonymous.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {

    @NotBlank(message = "Vui lòng nhập tên sản phẩm")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    String name;

    @Size(max = 50, message = "Mã vạch tối đa 50 ký tự")
    @Pattern(regexp = "^$|^[0-9A-Za-z-]+$", message = "Mã vạch chỉ gồm chữ, số và dấu gạch ngang")
    String barcode;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    String description;

    @Size(max = 255, message = "Chi tiết tối đa 255 ký tự")
    String details;

    @NotNull(message = "Vui lòng nhập VAT")
    @DecimalMin(value = "0", message = "VAT không được âm")
    @DecimalMax(value = "100", message = "VAT tối đa 100%")
    BigDecimal vat = BigDecimal.ZERO;

    @Min(value = 0, message = "Bảo hành không được âm")
    Integer warranty;

    String image;

    @NotNull(message = "Vui lòng nhập giá bán")
    @DecimalMin(value = "0", message = "Giá bán không được âm")
    BigDecimal salePrice = BigDecimal.ZERO;

    @NotNull(message = "Vui lòng nhập tồn tối thiểu")
    @Min(value = 0, message = "Tồn tối thiểu không được âm")
    Integer minStock = 0;

    @NotBlank(message = "Vui lòng chọn loại sản phẩm")
    String productCategoryId;

    String supplierId;

    @NotBlank(message = "Vui lòng chọn đơn vị tính")
    String unitId;

    String brandId;

}
