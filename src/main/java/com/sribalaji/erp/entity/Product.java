package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DUAL UNIT DESIGN
 * -----------------
 * purchaseUnit      -> the "big" unit you buy in, e.g. Coil, Box, Carton, Bag
 * saleUnit          -> the "small" unit you sell in, e.g. Meter, Piece, Kg
 * conversionFactor  -> how many saleUnits make ONE purchaseUnit
 *                      e.g. 1 Coil = 100 Meters  -> conversionFactor = 100
 *
 * currentStock is ALWAYS stored in purchaseUnit (as required).
 * When a cashier sells `qtyInSaleUnit`, the service layer converts:
 *      stockReductionInPurchaseUnit = qtyInSaleUnit / conversionFactor
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_hsn", columnList = "hsnCode")
})
@Getter
@Setter
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String hsnCode;

    public enum Category { KIRANA, HARDWARE, OTHER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.OTHER;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent; // 5, 12, 18, or 28

    @NotBlank
    @Column(nullable = false, length = 30)
    private String purchaseUnit; // e.g. "Coil", "Box", "Carton", "Bag"

    @NotBlank
    @Column(nullable = false, length = 30)
    private String saleUnit; // e.g. "Meter", "Piece", "Kg"

    /** How many saleUnits = 1 purchaseUnit. Must be > 0. If purchaseUnit == saleUnit, set to 1. */
    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice; // price per purchaseUnit

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice; // price per saleUnit

    /** ALWAYS in purchaseUnit. Source of truth for stock. */
    @NotNull
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal reorderLevel = BigDecimal.ZERO; // in purchaseUnit

    @Column(nullable = false)
    private boolean active = true;

    @Transient
    public boolean isLowStock() {
        return currentStock != null && reorderLevel != null
                && currentStock.compareTo(reorderLevel) <= 0;
    }
}
