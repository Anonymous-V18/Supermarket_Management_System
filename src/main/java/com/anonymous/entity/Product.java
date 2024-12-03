package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product extends AbstractEntity {

    String name;
    String description;
    String details;
    Double vat;
    Integer warranty;
    String image;

    @OneToMany(mappedBy = "product")
    Set<WarehouseProduct> warehouseProducts = new HashSet<>();

    @OneToMany(mappedBy = "product")
    Set<InvoiceDetail> invoiceDetails = new HashSet<>();

    @OneToMany(mappedBy = "product")
    Set<StockOutDetail> stockOutDetails = new HashSet<>();

    @OneToMany(mappedBy = "product")
    Set<StockInDetail> stockInDetails = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "product_category_id")
    ProductCategory productCategory;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    Unit unit;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    Brand brand;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    Promotion promotion;

}
