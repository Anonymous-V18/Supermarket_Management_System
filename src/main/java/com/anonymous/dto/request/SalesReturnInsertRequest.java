package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesReturnInsertRequest extends SalesReturnRequest {

    @NotBlank(message = "Vui lòng chọn hóa đơn")
    String invoiceId;

}
