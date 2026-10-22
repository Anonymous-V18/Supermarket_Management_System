package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
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
public class Product extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    String code;

    @Column(name = "barcode", unique = true, length = 50)
    String barcode;

    String name;
    String description;
    String details;
    BigDecimal vat;
    Integer warranty;
    String image;

    // Current retail price used for new invoices; stock-in lines update it
    BigDecimal salePrice;

    // Last purchase price, used for gross profit
    BigDecimal costPrice;

    // Below this quantity the product is reported as running low
    Integer minStock;

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
