package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_return_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sales_return_id", "product_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesReturnDetail extends AbstractEntity {

    Integer quantity;
    // What the customer actually paid per unit, after line and invoice discounts
    BigDecimal unitRefund;
    BigDecimal lineTotal;

    @ManyToOne
    @JoinColumn(name = "sales_return_id")
    SalesReturn salesReturn;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
