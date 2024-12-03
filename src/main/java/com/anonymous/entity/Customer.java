package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customer")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer extends AbstractEntity {
    
    String name;
    String gender;
    Instant dob;
    String phoneNumber;
    String email;
    Double accumulatedPoints;

    @OneToOne
    @JoinColumn(name = "user_id")
    User user;

    @OneToMany(mappedBy = "customer")
    Set<Invoice> invoices = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    Set<StockOut> stockOuts = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "address_id")
    Address address;
}
