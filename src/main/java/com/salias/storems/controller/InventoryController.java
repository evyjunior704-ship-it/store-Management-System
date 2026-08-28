package com.salias.storems.controller;

import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.InventoryService;
import com.salias.storems.service.ProductAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Manager stock controls. Every operation is scoped to the manager's own store.
 */
@Controller
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductAdminService productAdminService;

    public InventoryController(InventoryService inventoryService,
                               ProductAdminService productAdminService) {
        this.inventoryService = inventoryService;
        this.productAdminService = productAdminService;
    }

    @GetMapping("/inventory")
    String inventory(@RequestParam(name = "addProduct", required = false) String addProduct,
                     Model model) {
        Long storeId = CurrentUser.storeId();
        model.addAttribute("stock", inventoryService.findStock(storeId));
        model.addAttribute("lowStockThreshold", InventoryService.LOW_STOCK_THRESHOLD);
        // Catalogue products the manager can add to this store's shelves.
        model.addAttribute("catalogue", productAdminService.searchActive(addProduct));
        model.addAttribute("addProduct", addProduct);
        return "manager/inventory";
    }

    @PostMapping("/inventory/set-quantity")
    String setQuantity(@RequestParam Long productId,
                       @RequestParam int quantity,
                       RedirectAttributes flash) {
        try {
            inventoryService.setQuantity(CurrentUser.storeId(), productId, quantity);
            flash.addFlashAttribute("success", "Stock updated.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory";
    }

    @PostMapping("/inventory/adjust")
    String adjust(@RequestParam Long productId,
                  @RequestParam int delta,
                  RedirectAttributes flash) {
        try {
            inventoryService.adjustQuantity(CurrentUser.storeId(), productId, delta);
            flash.addFlashAttribute("success",
                    "Stock adjusted by %+d.".formatted(delta));
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory";
    }

    @PostMapping("/inventory/set-price")
    String setPrice(@RequestParam Long productId,
                    @RequestParam BigDecimal unitPrice,
                    RedirectAttributes flash) {
        try {
            inventoryService.setUnitPrice(CurrentUser.storeId(), productId, unitPrice);
            flash.addFlashAttribute("success", "Price updated.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory";
    }

    /** Puts a catalogue product on this store's shelves. */
    @PostMapping("/inventory/add-item")
    String addItem(@RequestParam Long productId,
                   @RequestParam(required = false) String inStoreName,
                   @RequestParam BigDecimal unitPrice,
                   @RequestParam(defaultValue = "0") int quantity,
                   RedirectAttributes flash) {
        try {
            inventoryService.addProductToStore(
                    CurrentUser.storeId(), productId, inStoreName, unitPrice, quantity);
            flash.addFlashAttribute("success", "Item added to this store's inventory.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory";
    }

    /** Takes a product off this store's shelves. Sales history is untouched. */
    @PostMapping("/inventory/remove-item")
    String removeItem(@RequestParam Long productId, RedirectAttributes flash) {
        inventoryService.removeProductFromStore(CurrentUser.storeId(), productId);
        flash.addFlashAttribute("success", "Item removed from this store's inventory.");
        return "redirect:/inventory";
    }
}
