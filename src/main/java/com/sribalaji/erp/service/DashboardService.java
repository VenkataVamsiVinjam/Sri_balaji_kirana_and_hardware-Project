package com.sribalaji.erp.service;

import com.sribalaji.erp.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceService invoiceService;
    private final PartyService partyService;
    private final ProductService productService;

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd-MMM");

    public DashboardDto buildDashboard() {
        BigDecimal todaysSales = invoiceService.todaysSales();
        BigDecimal totalOutstanding = partyService.totalOutstandingDues();
        long lowStockCount = productService.countLowStock();
        long totalProducts = productService.countActive();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay().minusNanos(1);
            labels.add(day.format(DAY_LABEL));
            values.add(invoiceService.salesBetween(start, end));
        }

        return new DashboardDto(todaysSales, totalOutstanding, lowStockCount, totalProducts, labels, values);
    }
}
