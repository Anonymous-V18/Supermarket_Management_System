package com.anonymous.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "warehouse_product")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseProduct extends AbstractEntity {

    Integer availableQuantity;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne
    @JoinColumn(name = "status_product_id")
    StatusProduct statusProduct;

}
