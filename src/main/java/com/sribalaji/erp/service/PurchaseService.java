package com.sribalaji.erp.service;

import com.sribalaji.erp.entity.*;
import com.sribalaji.erp.exception.BusinessException;
import com.sribalaji.erp.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductService productService;
    private final PartyService partyService;

    /**
     * Simple line item wrapper used by the controller to pass form data in.
     * Kept as a static inner record for simplicity rather than a full DTO file.
     */
    public record PurchaseLine(Long productId, BigDecimal quantityInPurchaseUnit, BigDecimal purchasePricePerUnit) {}

    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAllByOrderByPurchaseDateDesc();
    }

    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Purchase order not found: id=" + id));
    }

    /**
     * Creates a purchase entry:
     *   - Validates supplier + lines
     *   - Calculates taxable value, GST, and grand total per line and overall
     *   - INCREASES Product.currentStock by quantityInPurchaseUnit for each line (stock is stored in purchase unit,
     *     so no conversion needed here - the cashier/admin already enters purchase quantities in purchase unit)
     *   - INCREASES the supplier's outstandingBalance by the grand total (shop now owes the supplier)
     */
    public PurchaseOrder createPurchase(Long supplierId, String supplierInvoiceRef, List<PurchaseLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("Purchase must have at least one item.");
        }

        Party supplier = partyService.findById(supplierId);
        if (supplier.getPartyType() != Party.PartyType.SUPPLIER) {
            throw new BusinessException("Selected party is not a supplier.");
        }

        PurchaseOrder purchase = new PurchaseOrder();
        purchase.setSupplier(supplier);
        purchase.setSupplierInvoiceRef(supplierInvoiceRef);
        purchase.setPurchaseNumber(generatePurchaseNumber());

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (PurchaseLine line : lines) {
            if (line.quantityInPurchaseUnit() == null || line.quantityInPurchaseUnit().signum() <= 0) {
                throw new BusinessException("Purchase quantity must be greater than zero.");
            }
            Product product = productService.findById(line.productId());

            BigDecimal qty = line.quantityInPurchaseUnit();
            BigDecimal price = line.purchasePricePerUnit() != null ? line.purchasePricePerUnit() : product.getPurchasePrice();

            BigDecimal taxableValue = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gstAmount = taxableValue.multiply(product.getGstPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = taxableValue.add(gstAmount);

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantityInPurchaseUnit(qty);
            item.setPurchasePricePerUnit(price);
            item.setGstPercent(product.getGstPercent());
            item.setTaxableValue(taxableValue);
            item.setGstAmount(gstAmount);
            item.setLineTotal(lineTotal);
            purchase.addItem(item);

            totalTaxable = totalTaxable.add(taxableValue);
            totalGst = totalGst.add(gstAmount);

            // Stock is stored in purchaseUnit -> straightforward addition, no conversion needed.
            product.setCurrentStock(product.getCurrentStock().add(qty));
            // Optionally keep purchase price current for future reference
            product.setPurchasePrice(price);
        }

        purchase.setTotalTaxableValue(totalTaxable);
        purchase.setTotalGst(totalGst);
        purchase.setGrandTotal(totalTaxable.add(totalGst));

        purchaseOrderRepository.save(purchase);

        // Shop owes supplier more money now
        partyService.adjustOutstandingBalance(supplier.getId(), purchase.getGrandTotal());

        return purchase;
    }

    private String generatePurchaseNumber() {
        long count = purchaseOrderRepository.count() + 1;
        return String.format("PUR-%04d", count);
    }
}
