package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_in_detail",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"stock_in_id", "product_id"})})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockInDetail extends AbstractEntity {

    Integer quantity;
    BigDecimal inputPrice;
    BigDecimal salePrice;

    // Optional: only products that expire carry a lot and an expiry date
    @Column(name = "lot_number", length = 50)
    String lotNumber;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @ManyToOne
    @JoinColumn(name = "stock_in_id")
    StockIn stockIn;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
