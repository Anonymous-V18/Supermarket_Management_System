package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stock_transfer_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_transfer_id", "product_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTransferDetail extends AbstractEntity {

    Integer quantity;

    @ManyToOne
    @JoinColumn(name = "stock_transfer_id")
    StockTransfer stockTransfer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
