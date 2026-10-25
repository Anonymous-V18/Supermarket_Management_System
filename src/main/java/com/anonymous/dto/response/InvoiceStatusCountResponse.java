package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

// Invoices of one status created in a period; total is the amount on the invoices (returns not deducted)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceStatusCountResponse {

    String statusCode;

    String statusName;

    long invoiceCount;

    BigDecimal total;

}
