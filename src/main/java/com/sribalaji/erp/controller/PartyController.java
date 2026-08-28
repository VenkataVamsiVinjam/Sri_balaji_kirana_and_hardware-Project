package com.sribalaji.erp.controller;

import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("parties", partyService.searchCustomers(search));
        model.addAttribute("partyType", Party.PartyType.CUSTOMER);
        model.addAttribute("search", search);
        return "parties/list";
    }

    @GetMapping("/suppliers")
    public String suppliers(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("parties", partyService.searchSuppliers(search));
        model.addAttribute("partyType", Party.PartyType.SUPPLIER);
        model.addAttribute("search", search);
        return "parties/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam Party.PartyType type, Model model) {
        Party party = new Party();
        party.setPartyType(type);
        model.addAttribute("party", party);
        return "parties/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("party", partyService.findById(id));
        return "parties/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("party") Party party, BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "parties/form";
        }
        if (party.getId() != null) {
            partyService.update(party.getId(), party);
        } else {
            partyService.save(party);
        }
        redirectAttributes.addFlashAttribute("success", "Saved successfully.");
        return party.getPartyType() == Party.PartyType.CUSTOMER
                ? "redirect:/parties/customers" : "redirect:/parties/suppliers";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Party party = partyService.findById(id);
        Party.PartyType type = party.getPartyType();
        partyService.softDelete(id);
        redirectAttributes.addFlashAttribute("success", "Deactivated successfully.");
        return type == Party.PartyType.CUSTOMER ? "redirect:/parties/customers" : "redirect:/parties/suppliers";
    }
}
