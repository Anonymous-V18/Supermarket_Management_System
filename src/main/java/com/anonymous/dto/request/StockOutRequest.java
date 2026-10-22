package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

// Totals and prices are computed by the server
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class StockOutRequest {

    @NotBlank(message = "Vui lòng nhập lý do xuất kho")
    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    String reason;

    // Optional on create (defaults to PENDING)
    String statusInvoiceId;

    // Optional
    String customerId;

    // Optional: defaults to now
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
    @PastOrPresent(message = "Ngày xuất không được ở tương lai")
    Instant stockOutDate;

}
