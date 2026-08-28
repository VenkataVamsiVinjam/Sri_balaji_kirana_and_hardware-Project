package com.sribalaji.erp.controller;

import com.sribalaji.erp.dto.StockAdjustmentRequest;
import com.sribalaji.erp.entity.StockAdjustment;
import com.sribalaji.erp.entity.User;
import com.sribalaji.erp.security.CurrentUserResolver;
import com.sribalaji.erp.service.ProductService;
import com.sribalaji.erp.service.StockAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock-adjustment")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;
    private final ProductService productService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("products", productService.findAllActive());
        model.addAttribute("reasons", StockAdjustment.Reason.values());
        model.addAttribute("request", new StockAdjustmentRequest());
        return "stock/adjustment-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("request") StockAdjustmentRequest request,
                        RedirectAttributes redirectAttributes) {
        User currentUser = currentUserResolver.getCurrentUser();
        StockAdjustment saved = stockAdjustmentService.adjust(request, currentUser);
        redirectAttributes.addFlashAttribute("success",
                "Stock adjusted for '" + saved.getProduct().getName() + "'. New stock: "
                        + saved.getStockAfterAdjustment().stripTrailingZeros().toPlainString()
                        + " " + saved.getProduct().getPurchaseUnit());
        return "redirect:/stock-adjustment";
    }
}
