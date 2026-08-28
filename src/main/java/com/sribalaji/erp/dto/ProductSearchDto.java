package com.sribalaji.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDto {
    private Long id;
    private String name;
    private String hsnCode;
    private BigDecimal gstPercent;
    private String saleUnit;
    private String purchaseUnit;
    private BigDecimal conversionFactor;
    private BigDecimal sellingPrice;      // per saleUnit
    private BigDecimal availableStockInSaleUnit; // currentStock (purchaseUnit) * conversionFactor, for display only
}
