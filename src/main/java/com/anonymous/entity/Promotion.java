package com.anonymous.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion extends AbstractEntity {

    Instant startDate;
    Instant endDate;
    String name;
    String voucherCode;
    Integer limitVoucher;
    String description;

    @OneToMany(mappedBy = "promotion")
    Set<Product> products = new HashSet<>();
}
