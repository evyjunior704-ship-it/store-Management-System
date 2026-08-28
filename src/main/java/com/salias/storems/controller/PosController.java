package com.salias.storems.controller;

import com.salias.storems.model.inventoryitem.InventoryItem;
import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.InsufficientStockException;
import com.salias.storems.service.InventoryService;
import com.salias.storems.service.PosService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The till. Available to employees and to managers, who can also work a till.
 */
@Controller
public class PosController {

    private final PosService posService;
    private final InventoryService inventoryService;
    private final PosBasket basket;

    public PosController(PosService posService, InventoryService inventoryService, PosBasket basket) {
        this.posService = posService;
        this.inventoryService = inventoryService;
        this.basket = basket;
    }

    @GetMapping("/pos")
    String till(@RequestParam(name = "q", required = false) String query, Model model) {
        Long storeId = CurrentUser.storeId();

        List<InventoryItem> results = (query == null || query.isBlank())
                ? List.of()
                : posService.search(storeId, query);

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        model.addAttribute("basketLines", renderBasket(storeId));
        model.addAttribute("basketTotal", basketTotal(storeId));
        return "pos/till";
    }

    @PostMapping("/pos/add")
    String add(@RequestParam Long productId,
               @RequestParam(defaultValue = "1") int quantity,
               @RequestParam(name = "q", required = false) String query,
               RedirectAttributes flash) {
        if (quantity <= 0) {
            flash.addFlashAttribute("error", "Quantity must be at least 1.");
        } else {
            basket.add(productId, quantity);
        }
        return "redirect:/pos" + (query == null || query.isBlank() ? "" : "?q=" + query);
    }

    @PostMapping("/pos/update")
    String updateQuantity(@RequestParam Long productId, @RequestParam int quantity) {
        basket.setQuantity(productId, quantity);
        return "redirect:/pos";
    }

    @PostMapping("/pos/remove")
    String remove(@RequestParam Long productId) {
        basket.remove(productId);
        return "redirect:/pos";
    }

    @PostMapping("/pos/clear")
    String clear() {
        basket.clear();
        return "redirect:/pos";
    }

    @PostMapping("/pos/checkout")
    String checkout(RedirectAttributes flash) {
        if (basket.isEmpty()) {
            flash.addFlashAttribute("error", "The basket is empty.");
            return "redirect:/pos";
        }

        try {
            PosService.Receipt receipt = posService.checkout(
                    CurrentUser.storeId(), CurrentUser.employeeId(), basket.toBasketLines());
            basket.clear();
            flash.addFlashAttribute("success",
                    "Sale complete: %d line(s), total %s."
                            .formatted(receipt.lineCount(), receipt.total()));
        } catch (InsufficientStockException e) {
            // Nothing was committed: the whole checkout is one transaction.
            flash.addFlashAttribute("error", "Sale cancelled — " + e.getMessage());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", "Sale cancelled — " + e.getMessage());
        }
        return "redirect:/pos";
    }

    /** Joins the session basket to live prices and stock for display. */
    private List<PosBasket.Line> renderBasket(Long storeId) {
        List<PosBasket.Line> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : basket.contents().entrySet()) {
            InventoryItem item = inventoryService.findItem(storeId, entry.getKey());
            lines.add(new PosBasket.Line(
                    entry.getKey(),
                    item.getInStoreProductName(),
                    item.getUnitPrice(),
                    entry.getValue(),
                    item.getQuantity()));
        }
        return lines;
    }

    private BigDecimal basketTotal(Long storeId) {
        return renderBasket(storeId).stream()
                .map(PosBasket.Line::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
