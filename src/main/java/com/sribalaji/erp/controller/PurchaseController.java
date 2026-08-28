package com.sribalaji.erp.controller;

import com.sribalaji.erp.entity.PurchaseOrder;
import com.sribalaji.erp.service.PartyService;
import com.sribalaji.erp.service.ProductService;
import com.sribalaji.erp.service.PurchaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PartyService partyService;
    private final ProductService productService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("purchases", purchaseService.findAll());
        return "purchase/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("suppliers", partyService.findSuppliers());
        model.addAttribute("products", productService.findAllActive());
        return "purchase/form";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        PurchaseOrder po = purchaseService.findById(id);
        model.addAttribute("purchase", po);
        return "purchase/view";
    }

    /**
     * Form submits parallel arrays: productId[], quantity[], price[] since a purchase
     * can have any number of line items. Rows with a blank productId are skipped.
     */
    @PostMapping("/save")
    public String save(@RequestParam Long supplierId,
                        @RequestParam(required = false) String supplierInvoiceRef,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        String[] productIds = request.getParameterValues("productId[]");
        String[] quantities = request.getParameterValues("quantity[]");
        String[] prices = request.getParameterValues("price[]");

        List<PurchaseService.PurchaseLine> lines = new ArrayList<>();
        if (productIds != null) {
            for (int i = 0; i < productIds.length; i++) {
                if (productIds[i] == null || productIds[i].isBlank()) continue;
                Long productId = Long.valueOf(productIds[i]);
                BigDecimal qty = new BigDecimal(quantities[i]);
                BigDecimal price = new BigDecimal(prices[i]);
                lines.add(new PurchaseService.PurchaseLine(productId, qty, price));
            }
        }

        PurchaseOrder saved = purchaseService.createPurchase(supplierId, supplierInvoiceRef, lines);
        redirectAttributes.addFlashAttribute("success", "Purchase " + saved.getPurchaseNumber() + " recorded. Stock updated.");
        return "redirect:/purchase";
    }
}
