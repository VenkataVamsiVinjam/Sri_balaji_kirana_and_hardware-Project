package com.sribalaji.erp.controller.api;

import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.service.InvoiceService;
import com.sribalaji.erp.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PartyService partyService;
    private final InvoiceService invoiceService;

    @GetMapping("/customer/{customerId}/unpaid-invoices")
    public List<Map<String, Object>> unpaidInvoices(@PathVariable Long customerId) {
        Party customer = partyService.findById(customerId);
        List<Invoice> invoices = invoiceService.findUnpaidOrPartialForCustomer(customer);

        return invoices.stream().map(inv -> Map.<String, Object>of(
                "invoiceId", inv.getId(),
                "invoiceNumber", inv.getFormattedInvoiceNumber(),
                "invoiceDate", inv.getInvoiceDate().toLocalDate().toString(),
                "grandTotal", inv.getGrandTotal(),
                "amountPaid", inv.getAmountPaid(),
                "balanceDue", inv.getBalanceDue(),
                "paymentStatus", inv.getPaymentStatus().toString()
        )).collect(Collectors.toList());
    }
}
