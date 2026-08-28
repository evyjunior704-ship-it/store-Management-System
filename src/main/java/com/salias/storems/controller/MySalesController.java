package com.salias.storems.controller;

import com.salias.storems.report.DailySalesReport;
import com.salias.storems.report.ReportCsvWriter;
import com.salias.storems.report.ReportPdfWriter;
import com.salias.storems.report.SalesReportService;
import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.SalesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * An employee's own sales and end-of-shift report.
 *
 * <p>This is the "generated at logout" flow: the Log out link sends the employee
 * here first, so they see and can download their shift report before the session
 * ends. Because reports are derived from the sales table rather than stored,
 * skipping this screen loses nothing — the manager can still pull the same
 * figures later.
 */
@Controller
public class MySalesController {

    private final SalesService salesService;
    private final SalesReportService reportService;
    private final ReportCsvWriter csvWriter;
    private final ReportPdfWriter pdfWriter;

    public MySalesController(SalesService salesService,
                             SalesReportService reportService,
                             ReportCsvWriter csvWriter,
                             ReportPdfWriter pdfWriter) {
        this.salesService = salesService;
        this.reportService = reportService;
        this.csvWriter = csvWriter;
        this.pdfWriter = pdfWriter;
    }

    @GetMapping("/my-sales")
    String mySales(@RequestParam(required = false)
                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                   Model model) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        Long storeId = CurrentUser.storeId();
        Long employeeId = CurrentUser.employeeId();

        model.addAttribute("date", day);
        model.addAttribute("sales", salesService.findEmployeeSalesOn(storeId, employeeId, day));
        model.addAttribute("totals", salesService.totalsForEmployeeOn(storeId, employeeId, day));
        return "sales/my-sales";
    }

    /** Shift summary, shown on the way out. */
    @GetMapping("/my-sales/report")
    String shiftReport(@RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(name = "ending", defaultValue = "false") boolean ending,
                       Model model) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        DailySalesReport report =
                reportService.forEmployee(CurrentUser.storeId(), CurrentUser.employeeId(), day);

        model.addAttribute("report", report);
        model.addAttribute("ending", ending);
        return "sales/shift-report";
    }

    @GetMapping("/my-sales/report.csv")
    ResponseEntity<byte[]> downloadCsv(@RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        DailySalesReport report =
                reportService.forEmployee(CurrentUser.storeId(), CurrentUser.employeeId(), day);
        return csvDownload(report);
    }

    @GetMapping("/my-sales/report.pdf")
    ResponseEntity<byte[]> downloadPdf(@RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        DailySalesReport report =
                reportService.forEmployee(CurrentUser.storeId(), CurrentUser.employeeId(), day);
        return pdfDownload(report);
    }

    private ResponseEntity<byte[]> pdfDownload(DailySalesReport report) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s.pdf\"".formatted(report.filenameStem()))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfWriter.render(report));
    }

    private ResponseEntity<byte[]> csvDownload(DailySalesReport report) {
        byte[] body = csvWriter.render(report).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s.csv\"".formatted(report.filenameStem()))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
