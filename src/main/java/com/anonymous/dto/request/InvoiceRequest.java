package com.anonymous.dto.request;

import com.anonymous.constant.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

// Totals, prices and dates are computed by the server; clients only send what was sold
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class InvoiceRequest {

    // Optional: empty means a walk-in customer
    String customerId;

    // Optional on create (defaults to PENDING)
    String statusInvoiceId;

    // Invoice-level discount in money
    @DecimalMin(value = "0", message = "Giảm giá không được âm")
    BigDecimal discount = BigDecimal.ZERO;

    PaymentMethod paymentMethod = PaymentMethod.CASH;

    @DecimalMin(value = "0", message = "Tiền khách đưa không được âm")
    BigDecimal amountPaid;

}
