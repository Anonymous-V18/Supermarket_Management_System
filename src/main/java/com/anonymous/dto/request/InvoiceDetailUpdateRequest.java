package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetailUpdateRequest extends InvoiceDetailRequest {

    @NotBlank(message = "Thiếu mã dòng chứng từ")
    String id;

}
