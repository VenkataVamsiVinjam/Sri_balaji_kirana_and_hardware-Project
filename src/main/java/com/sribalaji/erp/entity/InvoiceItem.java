package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Quantity sold, entered & displayed in the product's SALE unit (e.g. Meters, Pieces). */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityInSaleUnit;

    /**
     * Stock reduction actually applied to Product.currentStock, in PURCHASE unit.
     * = quantityInSaleUnit / product.conversionFactor
     * Stored here for audit/traceability even though currentStock itself lives on Product.
     */
    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal stockReducedInPurchaseUnit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPricePerSaleUnit;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue; // qty * price, before this line's share of invoice discount

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal; // taxableValue + cgst + sgst
}
