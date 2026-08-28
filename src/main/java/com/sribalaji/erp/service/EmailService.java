package com.sribalaji.erp.service;

import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfService pdfService;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.name}")
    private String shopName;

    /**
     * Sends the invoice PDF to the customer's email, if present.
     * Runs asynchronously so the POS billing screen doesn't wait on SMTP round-trip.
     */
    @Async
    public void sendInvoiceEmail(Invoice invoice) {
        if (invoice.getCustomer() == null || invoice.getCustomer().getEmail() == null
                || invoice.getCustomer().getEmail().isBlank()) {
            log.info("Skipping invoice email for invoice {} - no customer email on file.", invoice.getFormattedInvoiceNumber());
            return;
        }

        try {
            byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(invoice.getCustomer().getEmail());
            helper.setSubject(shopName + " - Invoice " + invoice.getFormattedInvoiceNumber());
            helper.setText(buildEmailBody(invoice), false);
            helper.addAttachment(invoice.getFormattedInvoiceNumber() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Invoice email sent for {}", invoice.getFormattedInvoiceNumber());
        } catch (Exception e) {
            // Do not fail the billing transaction if email delivery fails - just log it.
            log.error("Failed to send invoice email for {}: {}", invoice.getFormattedInvoiceNumber(), e.getMessage());
        }
    }

    private String buildEmailBody(Invoice invoice) {
        return "Dear " + invoice.getCustomer().getName() + ",\n\n"
                + "Please find attached your invoice " + invoice.getFormattedInvoiceNumber()
                + " dated " + invoice.getInvoiceDate().toLocalDate() + ".\n\n"
                + "Grand Total: " + PdfService.formatInr(invoice.getGrandTotal()) + "\n"
                + "Balance Due: " + PdfService.formatInr(invoice.getBalanceDue()) + "\n\n"
                + "Thank you for shopping with us.\n\n"
                + shopName;
    }
}
