package com.sribalaji.erp.controller.api;

import com.sribalaji.erp.dto.CreateInvoiceRequest;
import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.User;
import com.sribalaji.erp.security.CurrentUserResolver;
import com.sribalaji.erp.service.EmailService;
import com.sribalaji.erp.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosApiController {

    private final InvoiceService invoiceService;
    private final EmailService emailService;
    private final CurrentUserResolver currentUserResolver;

    /**
     * Finalizes the sale: validates stock, converts dual units, calculates GST/discount,
     * reduces stock, updates customer dues, and generates the invoice number.
     * Called via AJAX from the POS screen without a page refresh.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CreateInvoiceRequest request) {
        User cashier = currentUserResolver.getCurrentUser();
        Invoice invoice = invoiceService.createInvoice(request, cashier);

        if (request.isEmailInvoice()) {
            emailService.sendInvoiceEmail(invoice);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "invoiceId", invoice.getId(),
                "invoiceNumber", invoice.getFormattedInvoiceNumber(),
                "grandTotal", invoice.getGrandTotal(),
                "balanceDue", invoice.getBalanceDue(),
                "pdfUrl", "/invoices/" + invoice.getId() + "/pdf"
        ));
    }
}
