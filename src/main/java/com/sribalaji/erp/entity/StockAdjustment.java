package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit trail for manual stock corrections that are NOT purchases or sales.
 * These entries must NEVER be counted in Purchase or Sales reports, but
 * must appear in the dedicated "Stock Adjustment History" report.
 */
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
public class StockAdjustment extends Auditable {

    public enum Reason { DAMAGED, EXPIRED, THEFT_LOSS, PHYSICAL_COUNT_CORRECTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Quantity adjusted, in the product's PURCHASE unit, as entered by the user.
     * Always stored as a POSITIVE magnitude; `reason` + business logic determines
     * whether stock goes down (DAMAGED/EXPIRED/THEFT_LOSS) or is corrected either way
     * (PHYSICAL_COUNT_CORRECTION uses signedQuantity below to know direction).
     */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    /**
     * The signed delta actually applied to Product.currentStock (negative = reduction,
     * positive = increase - only possible for PHYSICAL_COUNT_CORRECTION when the
     * physical count is higher than system stock).
     */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal signedQuantityApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    @Column(length = 300)
    private String remark;

    /** Stock level of the product immediately after this adjustment, for audit readability. */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal stockAfterAdjustment;

    @Column(nullable = false)
    private LocalDateTime adjustmentDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adjusted_by_user_id", nullable = false)
    private User adjustedBy;
}
