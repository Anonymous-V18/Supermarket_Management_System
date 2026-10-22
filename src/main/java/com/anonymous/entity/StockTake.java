package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// Physical count of a warehouse; completing it sets stock to the counted quantities
@Entity
@Table(name = "stock_take")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTake extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    String code;

    Instant stockTakeDate;
    String note;
    Integer totalDifference;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "stockTake")
    @Builder.Default
    Set<StockTakeDetail> stockTakeDetails = new HashSet<>();

}
