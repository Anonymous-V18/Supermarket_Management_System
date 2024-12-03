package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "address",
        uniqueConstraints = @UniqueConstraint(columnNames = {"street", "ward_id", "district_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address extends AbstractEntity {

    String street;

    @OneToOne(mappedBy = "address")
    Supplier supplier;

    @OneToOne(mappedBy = "address")
    Warehouse warehouse;

    @OneToMany(mappedBy = "address")
    Set<Employee> employees = new HashSet<>();

    @OneToMany(mappedBy = "address")
    Set<Customer> customers = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "ward_id")
    Ward ward;

    @ManyToOne
    @JoinColumn(name = "district_id")
    District district;

}
