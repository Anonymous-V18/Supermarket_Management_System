package com.anonymous.entity;

import com.anonymous.dto.response.RoleResponse;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AbstractEntity {

    @Column(name = "username", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String username;

    Boolean isActive;

    @OneToOne(mappedBy = "user")
    Customer customer;

    @OneToOne(mappedBy = "user")
    Employee employee;

    @Transient
    @Builder.Default
    Set<RoleResponse> roles = new HashSet<>();

}
