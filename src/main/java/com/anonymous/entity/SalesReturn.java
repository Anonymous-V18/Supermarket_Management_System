package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import com.anonymous.constant.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// Goods a customer brings back from a completed invoice; stock comes back unless the return is cancelled
@Entity
@Table(name = "sales_return")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesReturn extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    String code;

    Instant returnDate;
    String reason;
    Integer totalProduct;
    BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 20)
    PaymentMethod refundMethod;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    Invoice invoice;

    // Copied from the invoice so the return still shows who and where after the invoice changes
    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "salesReturn")
    @Builder.Default
    Set<SalesReturnDetail> salesReturnDetails = new HashSet<>();

}
