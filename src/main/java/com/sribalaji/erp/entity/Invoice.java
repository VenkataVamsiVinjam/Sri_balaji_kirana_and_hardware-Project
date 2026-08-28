package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends Auditable {

    public enum PaymentStatus { UNPAID, PARTIALLY_PAID, PAID }
    public enum DiscountType { FLAT, PERCENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Auto-incremented, unique, starts at 1. Formatted for display as INV-000001 in the UI layer. */
    @Column(nullable = false, unique = true)
    private Long invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id") // nullable => Walk-in customer
    private Party customer;

    @Column(length = 100)
    private String walkInCustomerName; // used only when customer is null

    @Column(nullable = false)
    private LocalDateTime invoiceDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiscountType discountType = DiscountType.FLAT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO; // raw value entered (₹ or %)

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // resolved ₹ amount actually applied

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO; // cumulative payments received against this invoice

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    @Transient
    public BigDecimal getBalanceDue() {
        return grandTotal.subtract(amountPaid);
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public String getFormattedInvoiceNumber() {
        return String.format("INV-%06d", invoiceNumber);
    }
}
