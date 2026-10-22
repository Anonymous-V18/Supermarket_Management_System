package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryResponse {

    LocalDate workDate;

    String shiftCode;

    String shiftName;

    Instant shiftStart;

    Instant shiftEnd;

    // EMPLOYEE: invoices of the logged-in employee, STORE: invoices of every employee (admin)
    String scope;

    // invoiceCount / revenue / productsSold cover every invoice except cancelled ones
    Long invoiceCount;

    BigDecimal revenue;

    Long productsSold;

    Long completedInvoiceCount;

    Long pendingInvoiceCount;

    Long cancelledInvoiceCount;

    List<ActivePromotion> activePromotions;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActivePromotion {

        String id;

        String name;

        String voucherCode;

        Instant endDate;

    }

}
