package com.sribalaji.erp.service;

import com.sribalaji.erp.entity.Product;
import com.sribalaji.erp.exception.BusinessException;
import com.sribalaji.erp.exception.ResourceNotFoundException;
import com.sribalaji.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAllActive() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Product> findByCategory(Product.Category category) {
        return productRepository.findByCategoryAndActiveTrueOrderByNameAsc(category);
    }

    public List<Product> search(String term) {
        if (term == null || term.isBlank()) {
            return findAllActive();
        }
        return productRepository.searchActiveByNameOrHsn(term.trim());
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: id=" + id));
    }

    public List<Product> findLowStock() {
        return productRepository.findLowStockProducts();
    }

    public long countLowStock() {
        return productRepository.countLowStockProducts();
    }

    public long countActive() {
        return productRepository.countByActiveTrue();
    }

    public Product save(Product product) {
        validate(product);
        return productRepository.save(product);
    }

    public Product update(Long id, Product incoming) {
        Product existing = findById(id);
        existing.setName(incoming.getName());
        existing.setHsnCode(incoming.getHsnCode());
        existing.setCategory(incoming.getCategory());
        existing.setGstPercent(incoming.getGstPercent());
        existing.setPurchaseUnit(incoming.getPurchaseUnit());
        existing.setSaleUnit(incoming.getSaleUnit());
        existing.setConversionFactor(incoming.getConversionFactor());
        existing.setPurchasePrice(incoming.getPurchasePrice());
        existing.setSellingPrice(incoming.getSellingPrice());
        existing.setReorderLevel(incoming.getReorderLevel());
        // NOTE: currentStock is intentionally NOT editable here.
        // It only changes via Purchase, Sale, or Stock Adjustment flows, to preserve audit integrity.
        validate(existing);
        return productRepository.save(existing);
    }

    /** Soft delete only - preserves referential integrity with historical invoices/purchases. ADMIN only (enforced at controller/security level). */
    public void softDelete(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private void validate(Product product) {
        if (product.getConversionFactor() == null || product.getConversionFactor().signum() <= 0) {
            throw new BusinessException("Conversion factor must be greater than zero.");
        }
        if (product.getGstPercent() == null || product.getGstPercent().signum() < 0) {
            throw new BusinessException("GST percent must be zero or greater.");
        }
    }
}
