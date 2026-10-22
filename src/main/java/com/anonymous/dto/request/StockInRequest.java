package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

// Totals are computed by the server from the lines
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class StockInRequest {

    // Optional on create (defaults to PENDING)
    String statusInvoiceId;

    // Optional: defaults to now
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
    @PastOrPresent(message = "Ngày nhập không được ở tương lai")
    Instant stockInDate;

}
