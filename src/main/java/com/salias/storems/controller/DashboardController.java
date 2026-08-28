package com.salias.storems.controller;

import com.salias.storems.report.SalesReportService;
import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.InventoryService;
import com.salias.storems.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class DashboardController {

    private final SalesService salesService;
    private final InventoryService inventoryService;
    private final SalesReportService reportService;

    public DashboardController(SalesService salesService,
                               InventoryService inventoryService,
                               SalesReportService reportService) {
        this.salesService = salesService;
        this.inventoryService = inventoryService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    String dashboard(Model model) {
        Long storeId = CurrentUser.storeId();
        LocalDate today = LocalDate.now();

        model.addAttribute("today", today);
        model.addAttribute("totals", salesService.totalsForStoreOn(storeId, today));
        model.addAttribute("lowStock", inventoryService.findLowStock(storeId));
        model.addAttribute("lowStockThreshold", InventoryService.LOW_STOCK_THRESHOLD);
        model.addAttribute("shiftReports", reportService.forAllSellersAtStore(storeId, today));
        return "manager/dashboard";
    }
}
