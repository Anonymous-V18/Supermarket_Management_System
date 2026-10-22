package com.anonymous.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTakeRequest {

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    String note;

    // Empty = PENDING; COMPLETED applies the differences to stock
    String statusInvoiceId;

    Instant stockTakeDate;

    @NotEmpty(message = "Phiếu kiểm kê phải có ít nhất một sản phẩm")
    @Valid
    List<StockTakeLineRequest> lines = new ArrayList<>();

}
