package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.BudgetComparisonResponse;
import com.smartfinancialexpenseanalysis.dto.CategoryReportResponse;
import com.smartfinancialexpenseanalysis.dto.FinancialReportResponse;
import com.smartfinancialexpenseanalysis.dto.PaymentMethodReportResponse;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.service.CsvExportService;
import com.smartfinancialexpenseanalysis.service.PdfExportService;
import com.smartfinancialexpenseanalysis.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Financial Reporting and Export.
 * All endpoints require session authentication and strictly enforce user ownership.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    public ReportController(ReportService reportService,
                            CsvExportService csvExportService,
                            PdfExportService pdfExportService) {
        this.reportService = reportService;
        this.csvExportService = csvExportService;
        this.pdfExportService = pdfExportService;
    }

    /**
     * Retrieves a monthly financial report for the authenticated user.
     *
     * @param month          month (1 - 12)
     * @param year           calendar year
     * @param categoryId     optional category filter
     * @param paymentMethod  optional payment method filter
     * @param authentication current security authentication
     * @return 200 OK with FinancialReportResponse
     */
    @GetMapping("/monthly")
    public ResponseEntity<FinancialReportResponse> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            Authentication authentication) {
        FinancialReportResponse response = reportService.generateMonthlyReport(
                authentication.getName(), month, year, categoryId, paymentMethod
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a custom date-range financial report for the authenticated user.
     *
     * @param startDate      start date (ISO format)
     * @param endDate        end date (ISO format)
     * @param categoryId     optional category filter
     * @param paymentMethod  optional payment method filter
     * @param authentication current security authentication
     * @return 200 OK with FinancialReportResponse
     */
    @GetMapping("/date-range")
    public ResponseEntity<FinancialReportResponse> getDateRangeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            Authentication authentication) {
        FinancialReportResponse response = reportService.generateDateRangeReport(
                authentication.getName(), startDate, endDate, categoryId, paymentMethod
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a category-wise breakdown report for a given date range.
     *
     * @param startDate      start date (ISO format)
     * @param endDate        end date (ISO format)
     * @param authentication current security authentication
     * @return 200 OK with list of CategoryReportResponse items
     */
    @GetMapping("/category")
    public ResponseEntity<List<CategoryReportResponse>> getCategoryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        List<CategoryReportResponse> response = reportService.getCategoryReport(
                authentication.getName(), startDate, endDate
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a payment method breakdown report for a given date range.
     *
     * @param startDate      start date (ISO format)
     * @param endDate        end date (ISO format)
     * @param authentication current security authentication
     * @return 200 OK with list of PaymentMethodReportResponse items
     */
    @GetMapping("/payment-method")
    public ResponseEntity<List<PaymentMethodReportResponse>> getPaymentMethodReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        List<PaymentMethodReportResponse> response = reportService.getPaymentMethodReport(
                authentication.getName(), startDate, endDate
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a budget vs actual spending comparison for a given month and year.
     *
     * @param month          month (1 - 12)
     * @param year           calendar year
     * @param authentication current security authentication
     * @return 200 OK with BudgetComparisonResponse
     */
    @GetMapping("/budget-vs-actual")
    public ResponseEntity<BudgetComparisonResponse> getBudgetVsActual(
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {
        BudgetComparisonResponse response = reportService.getBudgetComparison(
                authentication.getName(), month, year
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Exports a financial report in CSV format.
     *
     * @param month          optional month for monthly export
     * @param year           optional year for monthly export
     * @param startDate      optional start date for date-range export
     * @param endDate        optional end date for date-range export
     * @param categoryId     optional category filter
     * @param paymentMethod  optional payment method filter
     * @param authentication current security authentication
     * @return 200 OK with CSV file attachment
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            Authentication authentication) {

        FinancialReportResponse report = resolveReport(month, year, startDate, endDate, categoryId, paymentMethod, authentication.getName());
        String filename = resolveFilename("financial-report", month, year, startDate, endDate, "csv");
        byte[] csvBytes = csvExportService.generateCsv(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    /**
     * Exports a financial report in PDF format.
     *
     * @param month          optional month for monthly export
     * @param year           optional year for monthly export
     * @param startDate      optional start date for date-range export
     * @param endDate        optional end date for date-range export
     * @param categoryId     optional category filter
     * @param paymentMethod  optional payment method filter
     * @param authentication current security authentication
     * @return 200 OK with PDF file attachment
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            Authentication authentication) {

        FinancialReportResponse report = resolveReport(month, year, startDate, endDate, categoryId, paymentMethod, authentication.getName());
        String filename = resolveFilename("financial-report", month, year, startDate, endDate, "pdf");
        byte[] pdfBytes = pdfExportService.generatePdf(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private FinancialReportResponse resolveReport(Integer month, Integer year,
                                                  LocalDate startDate, LocalDate endDate,
                                                  Long categoryId, PaymentMethod paymentMethod,
                                                  String userEmail) {
        if (month != null && year != null) {
            return reportService.generateMonthlyReport(userEmail, month, year, categoryId, paymentMethod);
        } else if (startDate != null && endDate != null) {
            return reportService.generateDateRangeReport(userEmail, startDate, endDate, categoryId, paymentMethod);
        } else {
            throw new BadRequestException("Please specify either month and year, or startDate and endDate");
        }
    }

    private String resolveFilename(String prefix, Integer month, Integer year,
                                   LocalDate startDate, LocalDate endDate, String extension) {
        if (month != null && year != null) {
            return String.format("%s-%d-%02d.%s", prefix, year, month, extension);
        } else if (startDate != null && endDate != null) {
            return String.format("%s-%s-to-%s.%s", prefix, startDate, endDate, extension);
        } else {
            return String.format("%s.%s", prefix, extension);
        }
    }
}
