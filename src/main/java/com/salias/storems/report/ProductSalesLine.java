package com.salias.storems.report;

import java.math.BigDecimal;

/**
 * One product's contribution to a sales report. Projected directly from JPQL.
 */
public record ProductSalesLine(Long productId,
                               String productName,
                               long units,
                               BigDecimal revenue) {
}
