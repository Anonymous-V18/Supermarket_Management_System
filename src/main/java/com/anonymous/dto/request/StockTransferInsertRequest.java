package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTransferInsertRequest extends StockTransferRequest {

    @NotBlank(message = "Vui lòng chọn kho chuyển")
    String fromWarehouseId;

}
