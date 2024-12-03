package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Supplier extends AbstractEntity {

    String name;
    String email;
    String phoneNumber;
    String moreInfo;
    Instant contactDate;

    @OneToOne
    @JoinColumn(name = "address_id")
    Address address;

    @OneToMany(mappedBy = "supplier")
    Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy = "supplier")
    Set<StockIn> stockIns = new HashSet<>();
}
