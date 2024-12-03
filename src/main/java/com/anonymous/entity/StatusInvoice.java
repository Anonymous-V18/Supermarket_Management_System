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
@Table(name = "status_invoice")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusInvoice extends AbstractEntity {

    @Column(name = "name", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String name;
    @Column(name = "code", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String code;

    @OneToMany(mappedBy = "statusInvoice")
    Set<Invoice> invoices = new HashSet<>();

    @OneToMany(mappedBy = "statusInvoice")
    Set<StockIn> stockOuts = new HashSet<>();

    @OneToMany(mappedBy = "statusInvoice")
    Set<StockOut> stockIns = new HashSet<>();
}
