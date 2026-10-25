package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarehouseRevenueResponse {

    String warehouseId;

    String warehouseName;

    long invoiceCount;

    BigDecimal revenue;

    BigDecimal refunds;

    BigDecimal netRevenue;

    BigDecimal cost;

    BigDecimal grossProfit;

}
