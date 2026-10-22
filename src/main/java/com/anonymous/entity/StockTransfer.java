package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// PENDING: goods have left the source warehouse (in transit); COMPLETED: received at the destination
@Entity
@Table(name = "stock_transfer")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockTransfer extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    String code;

    Instant transferDate;
    String note;
    Integer totalProduct;

    @ManyToOne
    @JoinColumn(name = "from_warehouse_id")
    Warehouse fromWarehouse;

    @ManyToOne
    @JoinColumn(name = "to_warehouse_id")
    Warehouse toWarehouse;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "stockTransfer")
    @Builder.Default
    Set<StockTransferDetail> stockTransferDetails = new HashSet<>();

}
