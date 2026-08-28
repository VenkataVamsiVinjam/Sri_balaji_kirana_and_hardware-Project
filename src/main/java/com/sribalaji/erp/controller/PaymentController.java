package com.sribalaji.erp.controller;

import com.sribalaji.erp.dto.ReceivePaymentRequest;
import com.sribalaji.erp.service.PartyService;
import com.sribalaji.erp.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PartyService partyService;
    private final PaymentService paymentService;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("customers", partyService.findCustomersWithDues());
        model.addAttribute("request", new ReceivePaymentRequest());
        return "payments/receive";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("request") ReceivePaymentRequest request,
                        RedirectAttributes redirectAttributes) {
        paymentService.receivePayment(request);
        redirectAttributes.addFlashAttribute("success", "Payment received successfully.");
        return "redirect:/payments";
    }
}
