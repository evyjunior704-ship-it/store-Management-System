package com.salias.storems.report;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Renders a report as CSV. No dependencies: the format is simple enough that a
 * library would be more risk than help.
 */
@Component
public class ReportCsvWriter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String render(DailySalesReport report) {
        StringBuilder csv = new StringBuilder();

        csv.append("Daily Sales Report\n");
        line(csv, "Store", "%d - %s".formatted(report.storeId(), report.storeName()));
        line(csv, "Subject", report.subjectLabel());
        line(csv, "Date", report.date().toString());
        line(csv, "Generated", report.generatedAt().format(STAMP));
        csv.append('\n');

        line(csv, "Transactions", String.valueOf(report.totals().transactionCount()));
        line(csv, "Units sold", String.valueOf(report.totals().totalUnits()));
        line(csv, "Total revenue", report.totals().totalRevenue().toPlainString());
        line(csv, "Average transaction", report.totals().averageTransactionValue().toPlainString());
        csv.append('\n');

        csv.append("Product ID,Product,Units,Revenue\n");
        for (ProductSalesLine l : report.lines()) {
            csv.append(l.productId()).append(',')
                    .append(escape(l.productName())).append(',')
                    .append(l.units()).append(',')
                    .append(l.revenue().toPlainString()).append('\n');
        }

        if (report.lines().isEmpty()) {
            csv.append("(no sales recorded)\n");
        }

        return csv.toString();
    }

    private void line(StringBuilder csv, String key, String value) {
        csv.append(escape(key)).append(',').append(escape(value)).append('\n');
    }

    /** Quotes a field when it contains a comma, quote or newline; doubles inner quotes. */
    private String escape(String raw) {
        if (raw == null) {
            return "";
        }
        boolean needsQuotes = raw.indexOf(',') >= 0
                || raw.indexOf('"') >= 0
                || raw.indexOf('\n') >= 0
                || raw.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return raw;
        }
        return '"' + raw.replace("\"", "\"\"") + '"';
    }
}
