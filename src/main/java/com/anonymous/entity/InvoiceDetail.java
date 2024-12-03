package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    Double inputPrice;
    Double salePrice;
    Double promotionalPrice;
    Double percentDiscount;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

}
