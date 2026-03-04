package com.salias.storems.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalesDto(
        Long saleNumber,
        Long productId,
        Integer quantityBought,
        BigDecimal price,
        Long storeId,
        LocalDateTime madeAt
) {}
