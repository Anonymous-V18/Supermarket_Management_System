package com.anonymous.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "unit")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Unit extends AbstractEntity {

    String name;
    String code;

    @OneToMany(mappedBy = "unit")
    Set<Product> products = new HashSet<>();
}
