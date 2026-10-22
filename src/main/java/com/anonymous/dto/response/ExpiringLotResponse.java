package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

// Stock still on hand from one received lot (stock assumed to leave oldest receipt first)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpiringLotResponse {

    String warehouseId;

    String warehouseName;

    String productId;

    String productCode;

    String productName;

    String lotNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate expiryDate;

    long daysLeft;

    int quantity;

    String stockInCode;

    boolean expired;

}
