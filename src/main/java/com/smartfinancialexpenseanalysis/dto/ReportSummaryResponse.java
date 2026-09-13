package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * High-level summary metrics for a financial report.
 * All monetary amounts use BigDecimal with RoundingMode.HALF_UP.
 */
public class ReportSummaryResponse {

    private String reportTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalExpenses;
    private long expenseCount;
    private BigDecimal averageExpense;
    private BigDecimal highestExpense;
    private BigDecimal lowestExpense;

    public ReportSummaryResponse() {
    }

    public ReportSummaryResponse(String reportTitle, LocalDate startDate, LocalDate endDate,
                                 BigDecimal totalExpenses, long expenseCount,
                                 BigDecimal averageExpense, BigDecimal highestExpense,
                                 BigDecimal lowestExpense) {
        this.reportTitle = reportTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalExpenses = totalExpenses;
        this.expenseCount = expenseCount;
        this.averageExpense = averageExpense;
        this.highestExpense = highestExpense;
        this.lowestExpense = lowestExpense;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public long getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(long expenseCount) {
        this.expenseCount = expenseCount;
    }

    public BigDecimal getAverageExpense() {
        return averageExpense;
    }

    public void setAverageExpense(BigDecimal averageExpense) {
        this.averageExpense = averageExpense;
    }

    public BigDecimal getHighestExpense() {
        return highestExpense;
    }

    public void setHighestExpense(BigDecimal highestExpense) {
        this.highestExpense = highestExpense;
    }

    public BigDecimal getLowestExpense() {
        return lowestExpense;
    }

    public void setLowestExpense(BigDecimal lowestExpense) {
        this.lowestExpense = lowestExpense;
    }
}
