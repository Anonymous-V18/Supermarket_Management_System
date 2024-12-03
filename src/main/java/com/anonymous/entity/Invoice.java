package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice extends AbstractEntity {

    Instant invoiceCreateDate;
    Double discount;
    Integer totalProduct;
    Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "invoice")
    Set<InvoiceDetail> invoiceDetails = new HashSet<>();

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
