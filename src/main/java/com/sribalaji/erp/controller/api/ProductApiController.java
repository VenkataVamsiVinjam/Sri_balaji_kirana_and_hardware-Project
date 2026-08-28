package com.sribalaji.erp.controller.api;

import com.sribalaji.erp.dto.ProductSearchDto;
import com.sribalaji.erp.entity.Product;
import com.sribalaji.erp.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints backing the POS/Billing screen's live product search (AJAX, no page refresh),
 * as required.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    @GetMapping("/search")
    public List<ProductSearchDto> search(@RequestParam(required = false) String term) {
        List<Product> products = productService.search(term);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ProductSearchDto toDto(Product p) {
        return new ProductSearchDto(
                p.getId(),
                p.getName(),
                p.getHsnCode(),
                p.getGstPercent(),
                p.getSaleUnit(),
                p.getPurchaseUnit(),
                p.getConversionFactor(),
                p.getSellingPrice(),
                p.getCurrentStock().multiply(p.getConversionFactor()).setScale(2, RoundingMode.HALF_UP)
        );
    }
}
