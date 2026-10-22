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
public class InventoryRowResponse {

    String warehouseId;

    String warehouseName;

    String productId;

    String productCode;

    String productName;

    String barcode;

    String unitName;

    int quantity;

    int minStock;

    BigDecimal costPrice;

    BigDecimal salePrice;

    BigDecimal stockValue;

    boolean lowStock;

}
