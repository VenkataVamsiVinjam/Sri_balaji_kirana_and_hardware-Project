package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents money received from a customer against ONE specific invoice.
 * If a customer pays a lump sum against multiple invoices, the service layer
 * creates one Payment row per invoice it is applied to (oldest-first allocation).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends Auditable {

    public enum PaymentMode { CASH, UPI, CARD, BANK_TRANSFER, CHEQUE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Party customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode = PaymentMode.CASH;

    @Column(length = 300)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime paymentDate = LocalDateTime.now();
}
