package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    Double inputPrice;
    Double salePrice;

    @ManyToOne
    @JoinColumn(name = "stock_in_id")
    StockIn stockIn;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
