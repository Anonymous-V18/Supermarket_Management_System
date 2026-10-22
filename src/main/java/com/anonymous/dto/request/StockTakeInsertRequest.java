package com.anonymous.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTakeInsertRequest extends StockTakeRequest {

    @NotBlank(message = "Vui lòng chọn kho")
    String warehouseId;

}
