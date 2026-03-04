package com.salias.storems.dto.sales;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSaleRequest(
        @NotNull Long productId,
        @NotNull Integer quantityBought,
        @NotNull BigDecimal price,
        @NotNull Long storeId
) {}
