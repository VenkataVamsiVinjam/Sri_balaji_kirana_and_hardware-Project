package com.sribalaji.erp.service;

import com.sribalaji.erp.dto.GstSummaryRow;
import com.sribalaji.erp.dto.OutstandingReportRow;
import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.InvoiceItem;
import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.repository.InvoiceRepository;
import com.sribalaji.erp.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final PartyRepository partyRepository;

    /** Groups all sales between two dates by GST%, summing taxable value + CGST + SGST. */
    public List<GstSummaryRow> gstSummary(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Invoice> invoices = invoiceRepository.findForGstReport(start, end);

        // group by GST% across all invoice items in range
        Map<BigDecimal, GstSummaryRow> byRate = new TreeMap<>();

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal rate = item.getGstPercent();
                GstSummaryRow row = byRate.computeIfAbsent(rate, r ->
                        new GstSummaryRow(r, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                row.setTotalTaxableValue(row.getTotalTaxableValue().add(item.getTaxableValue()));
                row.setTotalCgst(row.getTotalCgst().add(item.getCgstAmount()));
                row.setTotalSgst(row.getTotalSgst().add(item.getSgstAmount()));
            }
        }

        List<GstSummaryRow> result = new ArrayList<>(byRate.values());
        for (GstSummaryRow row : result) {
            row.setTotalTax(row.getTotalCgst().add(row.getTotalSgst()));
            row.setTotalInvoiceValue(row.getTotalTaxableValue().add(row.getTotalTax()));
        }
        return result;
    }

    /** Customers with pending dues, bucketed by age of their OLDEST unpaid invoice. */
    public List<OutstandingReportRow> outstandingReport() {
        List<Party> customersWithDues = partyRepository.findCustomersWithOutstandingBalance();
        List<OutstandingReportRow> rows = new ArrayList<>();

        for (Party customer : customersWithDues) {
            List<Invoice> unpaidInvoices = invoiceRepository.findByCustomerAndPaymentStatusInOrderByInvoiceDateAsc(
                    customer, List.of(Invoice.PaymentStatus.UNPAID, Invoice.PaymentStatus.PARTIALLY_PAID));

            BigDecimal age0to30 = BigDecimal.ZERO;
            BigDecimal age30to60 = BigDecimal.ZERO;
            BigDecimal age60plus = BigDecimal.ZERO;

            for (Invoice invoice : unpaidInvoices) {
                long daysOld = ChronoUnit.DAYS.between(invoice.getInvoiceDate().toLocalDate(), LocalDate.now());
                BigDecimal due = invoice.getBalanceDue();
                if (daysOld <= 30) {
                    age0to30 = age0to30.add(due);
                } else if (daysOld <= 60) {
                    age30to60 = age30to60.add(due);
                } else {
                    age60plus = age60plus.add(due);
                }
            }

            rows.add(new OutstandingReportRow(
                    customer.getId(), customer.getName(), customer.getPhone(),
                    customer.getOutstandingBalance(), age0to30, age30to60, age60plus));
        }

        return rows;
    }
}
