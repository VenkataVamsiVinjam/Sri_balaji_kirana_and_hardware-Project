package com.sribalaji.erp.service;

import com.sribalaji.erp.dto.CreateInvoiceRequest;
import com.sribalaji.erp.dto.InvoiceItemRequest;
import com.sribalaji.erp.entity.*;
import com.sribalaji.erp.exception.BusinessException;
import com.sribalaji.erp.exception.ResourceNotFoundException;
import com.sribalaji.erp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;
    private final PartyService partyService;

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: id=" + id));
    }

    public List<Invoice> findBetween(LocalDateTime start, LocalDateTime end) {
        return invoiceRepository.findByInvoiceDateBetweenOrderByInvoiceDateDesc(start, end);
    }

    public List<Invoice> findUnpaidOrPartialForCustomer(Party customer) {
        return invoiceRepository.findByCustomerAndPaymentStatusInOrderByInvoiceDateAsc(
                customer, List.of(Invoice.PaymentStatus.UNPAID, Invoice.PaymentStatus.PARTIALLY_PAID));
    }

    public BigDecimal todaysSales() {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);
        BigDecimal total = invoiceRepository.sumSalesBetween(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal salesBetween(LocalDateTime start, LocalDateTime end) {
        BigDecimal total = invoiceRepository.sumSalesBetween(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Core billing method for the POS screen.
     *
     * DUAL UNIT LOGIC:
     *   Cashier enters quantityInSaleUnit (e.g. 10 Meters).
     *   stockReductionInPurchaseUnit = quantityInSaleUnit / product.conversionFactor
     *   (e.g. 10 Meters / 100 Meters-per-Coil = 0.10 Coils removed from stock)
     *   Product.currentStock (always in purchase unit) is reduced by that amount.
     *
     * DISCOUNT LOGIC:
     *   Discount (flat ₹ or %) is resolved to a ₹ amount on the pre-tax subtotal, then
     *   distributed proportionally across line items (by each line's share of the subtotal)
     *   so that GST is correctly calculated on the discounted taxable value per line.
     *
     * GST LOGIC:
     *   Intra-state sale assumed (Maharashtra shop, Maharashtra customers) -> GST split
     *   equally into CGST + SGST (e.g. 18% item = 9% CGST + 9% SGST).
     */
    public synchronized Invoice createInvoice(CreateInvoiceRequest request, User cashier) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty.");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDiscountType(request.getDiscountType());
        invoice.setDiscountValue(request.getDiscountValue() != null ? request.getDiscountValue() : BigDecimal.ZERO);

        Party customer = null;
        if (request.getCustomerId() != null) {
            customer = partyService.findById(request.getCustomerId());
            if (customer.getPartyType() != Party.PartyType.CUSTOMER) {
                throw new BusinessException("Selected party is not a customer.");
            }
            invoice.setCustomer(customer);
        } else {
            invoice.setWalkInCustomerName(
                    request.getWalkInCustomerName() != null && !request.getWalkInCustomerName().isBlank()
                            ? request.getWalkInCustomerName() : "Walk-in Customer");
        }

        // ---- Pass 1: resolve products, compute raw (pre-discount) taxable value per line ----
        record RawLine(Product product, BigDecimal qtyInSaleUnit, BigDecimal pricePerSaleUnit,
                        BigDecimal stockReductionInPurchaseUnit, BigDecimal rawTaxableValue) {}

        List<RawLine> rawLines = new ArrayList<>();
        BigDecimal subtotalRaw = BigDecimal.ZERO;

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = productService.findById(itemReq.getProductId());

            BigDecimal qty = itemReq.getQuantityInSaleUnit();
            if (qty == null || qty.signum() <= 0) {
                throw new BusinessException("Invalid quantity for product: " + product.getName());
            }

            BigDecimal price = itemReq.getSellingPricePerSaleUnitOverride() != null
                    ? itemReq.getSellingPricePerSaleUnitOverride()
                    : product.getSellingPrice();

            // ---- DUAL UNIT CONVERSION: sale unit -> purchase unit ----
            BigDecimal stockReduction = qty.divide(product.getConversionFactor(), 6, RoundingMode.HALF_UP);

            if (product.getCurrentStock().compareTo(stockReduction) < 0) {
                throw new BusinessException(String.format(
                        "Insufficient stock for '%s'. Available: %s %s, Required: %s %s",
                        product.getName(),
                        product.getCurrentStock().stripTrailingZeros().toPlainString(), product.getPurchaseUnit(),
                        stockReduction.stripTrailingZeros().toPlainString(), product.getPurchaseUnit()));
            }

            BigDecimal rawTaxableValue = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

            rawLines.add(new RawLine(product, qty, price, stockReduction, rawTaxableValue));
            subtotalRaw = subtotalRaw.add(rawTaxableValue);
        }

        // ---- Resolve discount amount in ₹ ----
        BigDecimal discountAmount;
        if (invoice.getDiscountType() == Invoice.DiscountType.PERCENT) {
            discountAmount = subtotalRaw.multiply(invoice.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = invoice.getDiscountValue();
        }
        if (discountAmount.compareTo(subtotalRaw) > 0) {
            discountAmount = subtotalRaw; // never discount below zero
        }
        if (discountAmount.signum() < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        invoice.setDiscountAmount(discountAmount);

        // ---- Pass 2: distribute discount proportionally, compute GST, build InvoiceItem rows, reduce stock ----
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;

        for (RawLine rl : rawLines) {
            BigDecimal discountShare = subtotalRaw.signum() == 0
                    ? BigDecimal.ZERO
                    : rl.rawTaxableValue().multiply(discountAmount)
                        .divide(subtotalRaw, 2, RoundingMode.HALF_UP);

            BigDecimal finalTaxableValue = rl.rawTaxableValue().subtract(discountShare);
            if (finalTaxableValue.signum() < 0) finalTaxableValue = BigDecimal.ZERO;

            BigDecimal gstAmount = finalTaxableValue.multiply(rl.product().getGstPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal cgst = gstAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal sgst = gstAmount.subtract(cgst);

            InvoiceItem item = new InvoiceItem();
            item.setProduct(rl.product());
            item.setQuantityInSaleUnit(rl.qtyInSaleUnit());
            item.setStockReducedInPurchaseUnit(rl.stockReductionInPurchaseUnit());
            item.setSellingPricePerSaleUnit(rl.pricePerSaleUnit());
            item.setGstPercent(rl.product().getGstPercent());
            item.setTaxableValue(finalTaxableValue);
            item.setCgstAmount(cgst);
            item.setSgstAmount(sgst);
            item.setLineTotal(finalTaxableValue.add(cgst).add(sgst));
            invoice.addItem(item);

            totalTaxable = totalTaxable.add(finalTaxableValue);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);

            // ---- Apply stock reduction (in purchase unit) ----
            rl.product().setCurrentStock(rl.product().getCurrentStock().subtract(rl.stockReductionInPurchaseUnit()));
        }

        invoice.setTotalTaxableValue(totalTaxable);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        BigDecimal grandTotal = totalTaxable.add(totalCgst).add(totalSgst).setScale(2, RoundingMode.HALF_UP);
        invoice.setGrandTotal(grandTotal);

        BigDecimal amountPaidNow = request.getAmountPaidNow() != null ? request.getAmountPaidNow() : BigDecimal.ZERO;
        if (amountPaidNow.compareTo(grandTotal) > 0) {
            amountPaidNow = grandTotal;
        }
        invoice.setAmountPaid(amountPaidNow);
        updatePaymentStatus(invoice);

        invoiceRepository.save(invoice);

        // ---- Update customer's outstanding balance (Udhaar) for the unpaid remainder ----
        if (customer != null) {
            BigDecimal balanceDue = grandTotal.subtract(amountPaidNow);
            if (balanceDue.signum() > 0) {
                partyService.adjustOutstandingBalance(customer.getId(), balanceDue);
            }
        }

        return invoice;
    }

    private void updatePaymentStatus(Invoice invoice) {
        if (invoice.getAmountPaid().compareTo(invoice.getGrandTotal()) >= 0) {
            invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
        } else if (invoice.getAmountPaid().signum() > 0) {
            invoice.setPaymentStatus(Invoice.PaymentStatus.PARTIALLY_PAID);
        } else {
            invoice.setPaymentStatus(Invoice.PaymentStatus.UNPAID);
        }
    }

    /** Applies an incoming payment to this invoice's amountPaid + status. Called by PaymentService. */
    void applyPayment(Invoice invoice, BigDecimal amount) {
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        updatePaymentStatus(invoice);
        invoiceRepository.save(invoice);
    }

    private long nextInvoiceNumber() {
        return invoiceRepository.findTopByOrderByInvoiceNumberDesc()
                .map(inv -> inv.getInvoiceNumber() + 1)
                .orElse(1L); // Auto-increment starting from 1, as required
    }
}
