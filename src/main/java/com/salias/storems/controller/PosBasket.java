package com.salias.storems.controller;

import com.salias.storems.service.PosService;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The till basket, held in the user's session while they build a sale.
 *
 * <p>Deliberately stores only product ids and quantities. Prices and names are
 * re-read from inventory when the basket is rendered and again at checkout, so a
 * stale session can never commit a stale price.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PosBasket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Insertion-ordered so the basket does not reshuffle as the cashier works. */
    private final Map<Long, Integer> quantitiesByProductId = new LinkedHashMap<>();

    /** A basket line joined to live pricing, for display only. */
    public record Line(Long productId, String name, BigDecimal unitPrice, int quantity, int available) {
        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        public boolean exceedsStock() {
            return quantity > available;
        }
    }

    public void add(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        quantitiesByProductId.merge(productId, quantity, Integer::sum);
    }

    public void setQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            quantitiesByProductId.remove(productId);
        } else {
            quantitiesByProductId.put(productId, quantity);
        }
    }

    public void remove(Long productId) {
        quantitiesByProductId.remove(productId);
    }

    public void clear() {
        quantitiesByProductId.clear();
    }

    public boolean isEmpty() {
        return quantitiesByProductId.isEmpty();
    }

    public int distinctItems() {
        return quantitiesByProductId.size();
    }

    public List<PosService.BasketLine> toBasketLines() {
        List<PosService.BasketLine> lines = new ArrayList<>(quantitiesByProductId.size());
        quantitiesByProductId.forEach((productId, qty) ->
                lines.add(new PosService.BasketLine(productId, qty)));
        return lines;
    }

    public Map<Long, Integer> contents() {
        return Map.copyOf(quantitiesByProductId);
    }
}
