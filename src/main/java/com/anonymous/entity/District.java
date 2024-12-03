package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "district")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class District extends AbstractEntity {
    
    String name;

    @OneToMany(mappedBy = "district")
    Set<Ward> wards = new HashSet<>();

    @OneToMany(mappedBy = "district")
    Set<Address> addresses = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "city_id")
    City city;


}
