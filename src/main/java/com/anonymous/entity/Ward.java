package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ward")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ward extends AbstractEntity {

    String name;

    @OneToMany(mappedBy = "ward")
    Set<Address> addresses = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "city_id")
    City city;

    @ManyToOne
    @JoinColumn(name = "district_id")
    District district;

}
