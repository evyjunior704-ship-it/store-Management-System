package com.salias.storems.report;

import java.math.BigDecimal;

/**
 * Aggregate sales figures for a window. Projected directly from JPQL.
 */
public record SalesTotals(long transactionCount, long totalUnits, BigDecimal totalRevenue) {

    public static SalesTotals empty() {
        return new SalesTotals(0L, 0L, BigDecimal.ZERO);
    }

    public BigDecimal averageTransactionValue() {
        if (transactionCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(BigDecimal.valueOf(transactionCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
