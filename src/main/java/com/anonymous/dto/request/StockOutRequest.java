package com.anonymous.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class StockOutRequest {

    Integer totalProduct = 0;
    Double totalPrice = 0.0;
    String reason;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
    Instant stockOutDate = Instant.now();
    String statusInvoiceId;
    String customerId;


}
