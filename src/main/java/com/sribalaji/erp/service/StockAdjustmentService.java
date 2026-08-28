package com.sribalaji.erp.service;

import com.sribalaji.erp.dto.StockAdjustmentRequest;
import com.sribalaji.erp.entity.Product;
import com.sribalaji.erp.entity.StockAdjustment;
import com.sribalaji.erp.entity.User;
import com.sribalaji.erp.exception.BusinessException;
import com.sribalaji.erp.repository.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IMPORTANT: This service ONLY touches Product.currentStock and the StockAdjustment audit table.
 * It never creates Invoice/InvoiceItem or PurchaseOrder/PurchaseOrderItem rows, so these
 * adjustments correctly stay OUT of Purchase and Sales reports, and only show up in the
 * dedicated "Stock Adjustment History" report.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockAdjustmentService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductService productService;

    public List<StockAdjustment> findAll() {
        return stockAdjustmentRepository.findAllByOrderByAdjustmentDateDesc();
    }

    public List<StockAdjustment> findBetween(LocalDateTime start, LocalDateTime end) {
        return stockAdjustmentRepository.findByAdjustmentDateBetweenOrderByAdjustmentDateDesc(start, end);
    }

    public List<StockAdjustment> findByProduct(Long productId) {
        return stockAdjustmentRepository.findByProductIdOrderByAdjustmentDateDesc(productId);
    }

    public StockAdjustment adjust(StockAdjustmentRequest request, User currentUser) {
        Product product = productService.findById(request.getProductId());

        // DAMAGED / EXPIRED / THEFT_LOSS are ALWAYS reductions, regardless of the `increase` flag.
        // Only PHYSICAL_COUNT_CORRECTION may legitimately increase stock (physical count found more than system).
        boolean isIncrease = request.getReason() == StockAdjustment.Reason.PHYSICAL_COUNT_CORRECTION
                && request.isIncrease();

        BigDecimal signedQty = isIncrease ? request.getQuantity() : request.getQuantity().negate();

        if (!isIncrease && product.getCurrentStock().add(signedQty).signum() < 0) {
            throw new BusinessException(String.format(
                    "Cannot adjust '%s' by -%s %s. Only %s %s currently in stock.",
                    product.getName(), request.getQuantity().stripTrailingZeros().toPlainString(), product.getPurchaseUnit(),
                    product.getCurrentStock().stripTrailingZeros().toPlainString(), product.getPurchaseUnit()));
        }

        product.setCurrentStock(product.getCurrentStock().add(signedQty));

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(product);
        adjustment.setQuantity(request.getQuantity());
        adjustment.setSignedQuantityApplied(signedQty);
        adjustment.setReason(request.getReason());
        adjustment.setRemark(request.getRemark());
        adjustment.setStockAfterAdjustment(product.getCurrentStock());
        adjustment.setAdjustmentDate(LocalDateTime.now());
        adjustment.setAdjustedBy(currentUser);

        return stockAdjustmentRepository.save(adjustment);
    }
}
