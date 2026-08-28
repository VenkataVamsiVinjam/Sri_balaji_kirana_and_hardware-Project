package com.sribalaji.erp.controller;

import com.sribalaji.erp.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosController {

    private final PartyService partyService;

    @GetMapping
    public String pos(Model model) {
        model.addAttribute("customers", partyService.findCustomers());
        return "pos/billing";
    }
}
