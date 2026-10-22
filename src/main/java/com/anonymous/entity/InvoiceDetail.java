package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"invoice_id", "product_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetail extends AbstractEntity {

    Integer quantity;
    // Prices are copied from the product when the line is written, so later price changes don't rewrite history
    BigDecimal inputPrice;
    BigDecimal salePrice;
    BigDecimal discountPercent;
    BigDecimal lineTotal;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
