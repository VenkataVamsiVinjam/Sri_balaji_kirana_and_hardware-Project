package com.sribalaji.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private BigDecimal todaysSales;
    private BigDecimal totalOutstandingDues;
    private long lowStockCount;
    private long totalProducts;

    /** Last 7 days, oldest first. */
    private List<String> last7DaysLabels;
    private List<BigDecimal> last7DaysSales;
}
