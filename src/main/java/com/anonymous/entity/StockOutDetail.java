package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stock_out_detail", uniqueConstraints = @UniqueConstraint(columnNames = {"stock_out_id", "product_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOutDetail extends AbstractEntity {

    Integer quantity;
    Double inputPrice;
    Double salePrice;
    Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "stock_out_id")
    StockOut stockOut;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;
    
}
