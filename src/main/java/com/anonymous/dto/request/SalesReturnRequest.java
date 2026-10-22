package com.anonymous.dto.request;

import com.anonymous.constant.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class SalesReturnRequest {

    @NotBlank(message = "Vui lòng nhập lý do trả hàng")
    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    String reason;

    // Empty = PENDING
    String statusInvoiceId;

    // Empty = same method the invoice was paid with
    PaymentMethod refundMethod;

    // Empty = now
    Instant returnDate;

    // The whole set of lines; replaces the previous lines on update
    @NotEmpty(message = "Phiếu trả phải có ít nhất một sản phẩm")
    @Valid
    List<DocumentLineRequest> lines = new ArrayList<>();

}
