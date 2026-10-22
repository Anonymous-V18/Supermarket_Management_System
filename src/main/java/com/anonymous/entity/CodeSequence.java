package com.anonymous.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Counter behind human-readable business codes (e.g. EMPLOYEE -> NV00001)
@Entity
@Table(name = "code_sequence")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CodeSequence {

    @Id
    @Column(name = "name", length = 50)
    String name;

    @Column(name = "prefix", nullable = false, length = 10)
    String prefix;

    @Column(name = "padding", nullable = false)
    Integer padding;

    @Column(name = "next_value", nullable = false)
    Long nextValue;

}
