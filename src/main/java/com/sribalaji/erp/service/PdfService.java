package com.sribalaji.erp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.InvoiceItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates a printable PDF invoice using iText.
 * Amounts are formatted using the Indian numbering system (e.g. ₹12,34,567.00)
 * via the "en-IN" Locale's NumberFormat, as required.
 */
@Service
public class PdfService {

    @Value("${app.name}")
    private String shopName;

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    private static final Font TOTAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

    private static final Locale INDIA = new Locale("en", "IN");

    public byte[] generateInvoicePdf(Invoice invoice) {
        try {
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document);
            addInvoiceMeta(document, invoice);
            addItemsTable(document, invoice);
            addTotals(document, invoice);
            addFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph(shopName, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph sub = new Paragraph("Maharashtra, India", NORMAL_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        document.add(sub);

        document.add(new Paragraph(" "));
        Paragraph taxInvoice = new Paragraph("TAX INVOICE", HEADER_FONT);
        taxInvoice.setAlignment(Element.ALIGN_CENTER);
        document.add(taxInvoice);
        document.add(new Paragraph(" "));
    }

    private void addInvoiceMeta(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        String customerName = invoice.getCustomer() != null ? invoice.getCustomer().getName() : invoice.getWalkInCustomerName();
        String customerGstin = invoice.getCustomer() != null && invoice.getCustomer().getGstin() != null
                ? invoice.getCustomer().getGstin() : "-";
        String customerPhone = invoice.getCustomer() != null && invoice.getCustomer().getPhone() != null
                ? invoice.getCustomer().getPhone() : "-";

        table.addCell(borderless("Invoice No: " + invoice.getFormattedInvoiceNumber(), HEADER_FONT));
        table.addCell(borderless("Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), NORMAL_FONT));

        table.addCell(borderless("Bill To: " + customerName, NORMAL_FONT));
        table.addCell(borderless("Phone: " + customerPhone, NORMAL_FONT));

        table.addCell(borderless("GSTIN: " + customerGstin, NORMAL_FONT));
        table.addCell(borderless("Payment Status: " + invoice.getPaymentStatus(), NORMAL_FONT));

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addItemsTable(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{3f, 1.3f, 1f, 1.2f, 1f, 1.2f, 1.2f, 1.3f});
        table.setWidthPercentage(100);

        String[] headers = {"Item", "HSN", "Qty", "Unit", "Rate", "Taxable", "GST", "Total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(new BaseColor(230, 230, 230));
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(cell(item.getProduct().getName(), SMALL_FONT));
            table.addCell(cell(item.getProduct().getHsnCode() != null ? item.getProduct().getHsnCode() : "-", SMALL_FONT));
            table.addCell(cell(item.getQuantityInSaleUnit().stripTrailingZeros().toPlainString(), SMALL_FONT));
            table.addCell(cell(item.getProduct().getSaleUnit(), SMALL_FONT));
            table.addCell(cell(formatInr(item.getSellingPricePerSaleUnit()), SMALL_FONT));
            table.addCell(cell(formatInr(item.getTaxableValue()), SMALL_FONT));
            table.addCell(cell(formatInr(item.getCgstAmount().add(item.getSgstAmount())), SMALL_FONT));
            table.addCell(cell(formatInr(item.getLineTotal()), SMALL_FONT));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalRow(table, "Taxable Value:", formatInr(invoice.getTotalTaxableValue()), NORMAL_FONT);
        addTotalRow(table, "CGST:", formatInr(invoice.getTotalCgst()), NORMAL_FONT);
        addTotalRow(table, "SGST:", formatInr(invoice.getTotalSgst()), NORMAL_FONT);
        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().signum() > 0) {
            addTotalRow(table, "Discount:", "- " + formatInr(invoice.getDiscountAmount()), NORMAL_FONT);
        }
        addTotalRow(table, "Grand Total:", formatInr(invoice.getGrandTotal()), TOTAL_FONT);
        addTotalRow(table, "Amount Paid:", formatInr(invoice.getAmountPaid()), NORMAL_FONT);
        addTotalRow(table, "Balance Due:", formatInr(invoice.getBalanceDue()), TOTAL_FONT);

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        Paragraph note = new Paragraph("Thank you for your business!", SMALL_FONT);
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);
        Paragraph computerGenerated = new Paragraph("This is a computer-generated invoice.", SMALL_FONT);
        computerGenerated.setAlignment(Element.ALIGN_CENTER);
        document.add(computerGenerated);
    }

    private PdfPCell borderless(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    /** Formats a BigDecimal as Indian currency, e.g. ₹12,34,567.00 */
    public static String formatInr(java.math.BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA);
        return formatter.format(amount);
    }
}
