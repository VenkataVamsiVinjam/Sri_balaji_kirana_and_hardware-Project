package com.sribalaji.erp.controller;

import com.sribalaji.erp.dto.DashboardDto;
import com.sribalaji.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardDto dashboard = dashboardService.buildDashboard();
        model.addAttribute("dashboard", dashboard);
        return "dashboard";
    }
}
