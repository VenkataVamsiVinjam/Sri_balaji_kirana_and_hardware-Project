package com.sribalaji.erp.dto;

import com.sribalaji.erp.entity.StockAdjustment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockAdjustmentRequest {

    @NotNull
    private Long productId;

    /** Always entered in PURCHASE unit, always a positive magnitude. */
    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    private StockAdjustment.Reason reason;

    /**
     * Only relevant for PHYSICAL_COUNT_CORRECTION: whether the physical count
     * found MORE stock than the system (increase) or LESS (decrease).
     * For DAMAGED / EXPIRED / THEFT_LOSS this is always treated as a decrease
     * regardless of what is sent.
     */
    private boolean increase = false;

    private String remark;
}
