package com.salias.storems.controller;

import com.salias.storems.report.DailySalesReport;
import com.salias.storems.report.ReportCsvWriter;
import com.salias.storems.report.ReportPdfWriter;
import com.salias.storems.report.SalesReportService;
import com.salias.storems.security.CurrentUser;
import com.salias.storems.service.EmployeeService;
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
 * Manager views over store sales, plus report downloads.
 *
 * <p>Reports are computed from the sales table on request, so the manager can
 * pull any employee's figures for any date whether or not that employee ever
 * clicked Log out.
 */
@Controller
public class SalesViewController {

    private final SalesService salesService;
    private final SalesReportService reportService;
    private final EmployeeService employeeService;
    private final ReportCsvWriter csvWriter;
    private final ReportPdfWriter pdfWriter;

    public SalesViewController(SalesService salesService,
                               SalesReportService reportService,
                               EmployeeService employeeService,
                               ReportCsvWriter csvWriter,
                               ReportPdfWriter pdfWriter) {
        this.salesService = salesService;
        this.reportService = reportService;
        this.employeeService = employeeService;
        this.csvWriter = csvWriter;
        this.pdfWriter = pdfWriter;
    }

    @GetMapping("/sales")
    String sales(@RequestParam(required = false)
                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                 @RequestParam(required = false) Long employeeId,
                 Model model) {
        Long storeId = CurrentUser.storeId();
        LocalDate day = (date == null) ? LocalDate.now() : date;

        model.addAttribute("date", day);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("staff", employeeService.findStaff(storeId));
        model.addAttribute("sales", employeeId == null
                ? salesService.findStoreSalesOn(storeId, day)
                : salesService.findEmployeeSalesOn(storeId, employeeId, day));
        model.addAttribute("totals", employeeId == null
                ? salesService.totalsForStoreOn(storeId, day)
                : salesService.totalsForEmployeeOn(storeId, employeeId, day));
        return "manager/sales";
    }

    @GetMapping("/reports")
    String reports(@RequestParam(required = false)
                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                   Model model) {
        Long storeId = CurrentUser.storeId();
        LocalDate day = (date == null) ? LocalDate.now() : date;

        model.addAttribute("date", day);
        model.addAttribute("storeReport", reportService.forStore(storeId, day));
        model.addAttribute("employeeReports", reportService.forAllSellersAtStore(storeId, day));
        return "manager/reports";
    }

    @GetMapping("/reports/store.csv")
    ResponseEntity<byte[]> storeCsv(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        return csvDownload(reportService.forStore(CurrentUser.storeId(), day));
    }

    @GetMapping("/reports/employee.csv")
    ResponseEntity<byte[]> employeeCsv(@RequestParam Long employeeId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        Long storeId = CurrentUser.storeId();
        // Scope check: refuses employees who do not work at this manager's store.
        employeeService.findAtStore(storeId, employeeId);
        return csvDownload(reportService.forEmployee(storeId, employeeId, day));
    }

    @GetMapping("/reports/store.pdf")
    ResponseEntity<byte[]> storePdf(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        return pdfDownload(reportService.forStore(CurrentUser.storeId(), day));
    }

    @GetMapping("/reports/employee.pdf")
    ResponseEntity<byte[]> employeePdf(@RequestParam Long employeeId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date == null) ? LocalDate.now() : date;
        Long storeId = CurrentUser.storeId();
        employeeService.findAtStore(storeId, employeeId);
        return pdfDownload(reportService.forEmployee(storeId, employeeId, day));
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
