package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stock_out")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOut extends AbstractEntity {

    Integer totalProduct;
    Double totalPrice;
    String reason;
    Instant stockOutDate;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "stockOut")
    Set<StockOutDetail> stockOutDetails = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;
}
