package com.anonymous.entity;

import com.anonymous.constant.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
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
public class Invoice extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    String code;

    Instant invoiceCreateDate;
    // Invoice-level discount in money; line discounts are on InvoiceDetail
    BigDecimal discount;
    Integer totalProduct;
    BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    PaymentMethod paymentMethod;

    BigDecimal amountPaid;

    BigDecimal changeAmount;

    @ManyToOne
    @JoinColumn(name = "status_invoice_id")
    StatusInvoice statusInvoice;

    @OneToMany(mappedBy = "invoice")
    @Builder.Default
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
