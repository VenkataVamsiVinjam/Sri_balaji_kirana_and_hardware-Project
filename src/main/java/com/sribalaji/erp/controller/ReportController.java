package com.sribalaji.erp.controller;

import com.sribalaji.erp.entity.Product;
import com.sribalaji.erp.service.ProductService;
import com.sribalaji.erp.service.ReportService;
import com.sribalaji.erp.service.StockAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ProductService productService;
    private final StockAdjustmentService stockAdjustmentService;
    private final ReportService reportService;

    @GetMapping("/stock")
    public String stockReport(@RequestParam(required = false) Product.Category category, Model model) {
        model.addAttribute("products", category != null
                ? productService.findByCategory(category) : productService.findAllActive());
        model.addAttribute("categories", Product.Category.values());
        model.addAttribute("selectedCategory", category);
        return "reports/stock";
    }

    @GetMapping("/stock-adjustment-history")
    public String stockAdjustmentHistory(Model model) {
        model.addAttribute("adjustments", stockAdjustmentService.findAll());
        return "reports/stock-adjustment-history";
    }

    @GetMapping("/gst-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public String gstSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        model.addAttribute("rows", reportService.gstSummary(from, to));
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "reports/gst-summary";
    }

    @GetMapping("/outstanding")
    @PreAuthorize("hasRole('ADMIN')")
    public String outstanding(Model model) {
        model.addAttribute("rows", reportService.outstandingReport());
        return "reports/outstanding";
    }
}
