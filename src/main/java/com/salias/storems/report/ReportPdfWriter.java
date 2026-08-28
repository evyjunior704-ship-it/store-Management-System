package com.salias.storems.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Renders a report as a single-page PDF.
 */
@Component
public class ReportPdfWriter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

    public byte[] render(DailySalesReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 46, 46, 46, 46);

        PdfWriter.getInstance(document, out);
        document.open();

        document.add(paragraph("Daily Sales Report", H1, 2));
        document.add(paragraph("%s (store #%d)".formatted(report.storeName(), report.storeId()), BODY, 0));
        document.add(paragraph(report.subjectLabel() + " · " + report.date(), BODY, 0));
        document.add(paragraph("Generated " + report.generatedAt().format(STAMP), MUTED, 14));

        document.add(paragraph("Summary", H2, 6));
        PdfPTable summary = new PdfPTable(new float[]{2, 1});
        summary.setWidthPercentage(52);
        summaryRow(summary, "Transactions", String.valueOf(report.totals().transactionCount()));
        summaryRow(summary, "Units sold", String.valueOf(report.totals().totalUnits()));
        summaryRow(summary, "Total revenue", money(report.totals().totalRevenue()));
        summaryRow(summary, "Average sale", money(report.totals().averageTransactionValue()));
        document.add(summary);

        document.add(paragraph("By product", H2, 6));
        if (report.lines().isEmpty()) {
            document.add(paragraph("No sales recorded.", MUTED, 0));
        } else {
            PdfPTable table = new PdfPTable(new float[]{5, 1.4f, 2});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            header(table, "Product");
            header(table, "Units");
            header(table, "Revenue");
            for (ProductSalesLine line : report.lines()) {
                table.addCell(cell(line.productName(), Element.ALIGN_LEFT));
                table.addCell(cell(String.valueOf(line.units()), Element.ALIGN_RIGHT));
                table.addCell(cell(money(line.revenue()), Element.ALIGN_RIGHT));
            }
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }

    private Paragraph paragraph(String text, Font font, float spacingAfter) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(spacingAfter);
        return p;
    }

    private void summaryRow(PdfPTable table, String label, String value) {
        table.addCell(cell(label, Element.ALIGN_LEFT));
        table.addCell(cell(value, Element.ALIGN_RIGHT));
    }

    private void header(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, H2));
        cell.setBackgroundColor(new Color(240, 241, 243));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private PdfPCell cell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, BODY));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private String money(java.math.BigDecimal amount) {
        return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
