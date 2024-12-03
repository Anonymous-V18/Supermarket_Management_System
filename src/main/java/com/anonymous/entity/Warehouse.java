package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Warehouse extends AbstractEntity {

    String name;
    String email;
    String phoneNumber;
    String moreInfo;
    Instant establishDate;

    @OneToOne
    @JoinColumn(name = "address_id")
    Address address;

    @OneToMany(mappedBy = "warehouse")
    Set<WarehouseProduct> warehouseProducts = new HashSet<>();

    @OneToMany(mappedBy = "warehouse")
    Set<StockIn> stockIns = new HashSet<>();

    @OneToMany(mappedBy = "warehouse")
    Set<StockOut> stockOuts = new HashSet<>();

    @OneToMany(mappedBy = "warehouse")
    Set<Invoice> invoices = new HashSet<>();
}
