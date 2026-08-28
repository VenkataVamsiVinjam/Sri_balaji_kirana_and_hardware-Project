package com.sribalaji.erp.controller;

import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.service.EmailService;
import com.sribalaji.erp.service.InvoiceService;
import com.sribalaji.erp.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private final EmailService emailService;

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.findById(id));
        return "pos/invoice-view";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Invoice invoice = invoiceService.findById(id);
        byte[] pdf = pdfService.generateInvoicePdf(invoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", invoice.getFormattedInvoiceNumber() + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/{id}/email")
    public String emailInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Invoice invoice = invoiceService.findById(id);
        emailService.sendInvoiceEmail(invoice);
        redirectAttributes.addFlashAttribute("success", "Invoice email queued for sending.");
        return "redirect:/invoices/" + id;
    }
}
