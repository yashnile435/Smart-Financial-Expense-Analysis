package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ExpenseReportItemResponse;
import com.smartfinancialexpenseanalysis.dto.FinancialReportResponse;
import com.smartfinancialexpenseanalysis.dto.ReportSummaryResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service for generating compliant RFC 4180 CSV exports of financial reports.
 * Properly escapes commas, quotes, and newlines.
 * Completely excludes passwords, user IDs, or sensitive security data.
 */
@Service
public class CsvExportService {

    /**
     * Converts a FinancialReportResponse into a CSV byte array.
     *
     * @param report the complete financial report
     * @return byte array containing formatted CSV content in UTF-8
     */
    public byte[] generateCsv(FinancialReportResponse report) {
        StringBuilder sb = new StringBuilder();

        // Optional metadata header comments
        ReportSummaryResponse summary = report.getSummary();
        if (summary != null) {
            sb.append("# ").append(summary.getReportTitle()).append("\r\n");
            sb.append("# Date Range: ").append(summary.getStartDate()).append(" to ").append(summary.getEndDate()).append("\r\n");
            sb.append("# Total Expenses: ₹").append(summary.getTotalExpenses()).append("\r\n");
            sb.append("# Total Transactions: ").append(summary.getExpenseCount()).append("\r\n");
            sb.append("# Average Expense: ₹").append(summary.getAverageExpense()).append("\r\n");
            sb.append("# Highest Expense: ₹").append(summary.getHighestExpense()).append("\r\n");
            sb.append("# Lowest Expense: ₹").append(summary.getLowestExpense()).append("\r\n");
            sb.append("\r\n");
        }

        // Standard CSV Columns
        sb.append("Date,Category,Description,Payment Method,Amount\r\n");

        if (report.getExpenses() != null) {
            for (ExpenseReportItemResponse item : report.getExpenses()) {
                sb.append(item.getDate()).append(",");
                sb.append(escapeCsv(item.getCategory())).append(",");
                sb.append(escapeCsv(item.getDescription())).append(",");
                sb.append(item.getPaymentMethod() != null ? item.getPaymentMethod().name() : "OTHER").append(",");
                sb.append(item.getAmount() != null ? item.getAmount().toPlainString() : "0.00");
                sb.append("\r\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Escapes a single string field according to RFC 4180 rules.
     * Encloses strings in double quotes if they contain commas, double quotes, or newlines,
     * escaping existing double quotes with double double-quotes.
     *
     * @param value raw string value
     * @return RFC 4180 safe CSV string
     */
    public String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (containsSpecial) {
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return value;
    }
}
