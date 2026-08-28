package com.sribalaji.erp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceItemRequest {

    @NotNull
    private Long productId;

    /** Entered by the cashier IN SALE UNIT (e.g. 10 Meters). */
    @NotNull
    @Positive
    private BigDecimal quantityInSaleUnit;

    /**
     * Optional override of selling price per sale unit (e.g. manual discount on a line).
     * If null, the product's current sellingPrice is used.
     */
    private BigDecimal sellingPricePerSaleUnitOverride;
}
