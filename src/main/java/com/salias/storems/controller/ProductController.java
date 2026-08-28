package com.salias.storems.controller;

import com.salias.storems.model.product.Product;
import com.salias.storems.service.ProductAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue management. Manager-only.
 */
@Controller
public class ProductController {

    private final ProductAdminService productAdminService;

    public ProductController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping("/products")
    String products(Model model) {
        List<Product> all = productAdminService.findAll();

        // Tells the template which products can be hard-deleted rather than only
        // discontinued, so it can show the right button.
        Map<Long, Boolean> deletable = new LinkedHashMap<>();
        for (Product p : all) {
            deletable.put(p.getProductId(), productAdminService.isDeletable(p.getProductId()));
        }

        model.addAttribute("products", all);
        model.addAttribute("deletable", deletable);
        return "manager/products";
    }

    @PostMapping("/products")
    String create(@RequestParam String name,
                  @RequestParam(required = false) String manufacturer,
                  @RequestParam String description,
                  RedirectAttributes flash) {
        productAdminService.create(name, manufacturer, description);
        flash.addFlashAttribute("success", "Product '%s' added to the catalogue.".formatted(name));
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/update")
    String update(@PathVariable Long id,
                  @RequestParam String name,
                  @RequestParam(required = false) String manufacturer,
                  @RequestParam String description,
                  RedirectAttributes flash) {
        productAdminService.update(id, name, manufacturer, description);
        flash.addFlashAttribute("success", "Product updated.");
        return "redirect:/products";
    }

    /** Soft removal: keeps sales history, hides the product everywhere else. */
    @PostMapping("/products/{id}/discontinue")
    String discontinue(@PathVariable Long id, RedirectAttributes flash) {
        Product product = productAdminService.discontinue(id);
        flash.addFlashAttribute("success",
                "'%s' discontinued. Its sales history is intact.".formatted(product.getProductName()));
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/reinstate")
    String reinstate(@PathVariable Long id, RedirectAttributes flash) {
        Product product = productAdminService.reinstate(id);
        flash.addFlashAttribute("success", "'%s' is back in the catalogue."
                .formatted(product.getProductName()));
        return "redirect:/products";
    }

    /** Hard delete, only permitted while nothing references the product. */
    @PostMapping("/products/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            productAdminService.deletePermanently(id);
            flash.addFlashAttribute("success", "Product deleted permanently.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products";
    }
}
