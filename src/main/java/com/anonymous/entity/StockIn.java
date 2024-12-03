package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stock_in")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockIn extends AbstractEntity {

    Integer totalProduct;
    Double totalPrice;
    Instant stockInDate;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "stockIn")
    Set<StockInDetail> stockInDetails = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

}
