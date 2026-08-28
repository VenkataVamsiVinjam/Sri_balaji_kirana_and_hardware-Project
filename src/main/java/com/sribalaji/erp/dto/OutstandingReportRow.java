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
public class OutstandingReportRow {
    private Long partyId;
    private String customerName;
    private String phone;
    private BigDecimal totalOutstanding;
    private BigDecimal age0to30;
    private BigDecimal age30to60;
    private BigDecimal age60plus;
}
