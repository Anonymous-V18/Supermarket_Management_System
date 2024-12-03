package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employee")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Employee extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String code;
    String name;
    String gender;
    Instant dob;
    String phoneNumber;
    String email;

    @OneToOne
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @JoinColumn(name = "position_id")
    Position position;

    @OneToMany(mappedBy = "employee")
    Set<Invoice> invoices = new HashSet<>();

    @OneToMany(mappedBy = "employee")
    Set<StockOut> stockOuts = new HashSet<>();

    @OneToMany(mappedBy = "employee")
    Set<StockIn> stockIns = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "address_id")
    Address address;
}
