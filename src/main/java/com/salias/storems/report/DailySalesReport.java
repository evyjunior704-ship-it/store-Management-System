package com.salias.storems.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A day's sales for one employee, or for a whole store when
 * {@code employeeId} is null.
 *
 * <p>Computed on demand from the {@code sales} table rather than stored, so a
 * missed logout cannot lose data.
 */
public record DailySalesReport(LocalDate date,
                               Long storeId,
                               String storeName,
                               Long employeeId,
                               String employeeName,
                               SalesTotals totals,
                               List<ProductSalesLine> lines,
                               LocalDateTime generatedAt) {

    public boolean isStoreWide() {
        return employeeId == null;
    }

    public String subjectLabel() {
        return isStoreWide() ? "Whole store" : employeeName;
    }

    /** Filename stem, e.g. {@code sales-report_store-3_ada.kembou_2026-08-27}. */
    public String filenameStem() {
        String who = isStoreWide()
                ? "store-wide"
                : ("employee-" + employeeId);
        return "sales-report_store-%d_%s_%s".formatted(storeId, who, date);
    }
}
