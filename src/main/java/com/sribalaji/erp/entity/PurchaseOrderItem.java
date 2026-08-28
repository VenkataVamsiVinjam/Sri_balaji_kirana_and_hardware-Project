package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Quantity purchased, always entered & stored in the product's PURCHASE unit (e.g. Coils, Boxes). */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityInPurchaseUnit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePricePerUnit;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue; // quantity * price

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal gstAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal; // taxableValue + gstAmount
}
