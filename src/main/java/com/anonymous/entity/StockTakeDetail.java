package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stock_take_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_take_id", "product_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTakeDetail extends AbstractEntity {

    // Stock on record: refreshed on every save, frozen when the count is completed
    Integer systemQuantity;
    Integer countedQuantity;
    Integer difference;

    @ManyToOne
    @JoinColumn(name = "stock_take_id")
    StockTake stockTake;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
