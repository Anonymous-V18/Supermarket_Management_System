package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

// What is still returnable from one invoice line
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnableLineResponse {

    String productId;

    String productCode;

    String productName;

    Integer soldQuantity;

    Integer returnedQuantity;

    Integer returnableQuantity;

    BigDecimal unitRefund;

}
