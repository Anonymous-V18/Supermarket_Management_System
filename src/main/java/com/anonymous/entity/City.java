package com.anonymous.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "city")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class City extends AbstractEntity {

    @Column(name = "name", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String name;

    @OneToMany(mappedBy = "city")
    Set<District> districts = new HashSet<>();

    @OneToMany(mappedBy = "city")
    Set<Ward> wards = new HashSet<>();

}
