package com.smartfinancialexpenseanalysis.dto;

import java.util.List;

/**
 * Complete financial report response payload combining summary metrics,
 * category breakdown, payment method breakdown, budget comparison, and transaction details.
 */
public class FinancialReportResponse {

    private ReportSummaryResponse summary;
    private List<CategoryReportResponse> categoryBreakdown;
    private List<PaymentMethodReportResponse> paymentMethodBreakdown;
    private BudgetComparisonResponse budgetComparison;
    private List<ExpenseReportItemResponse> expenses;

    public FinancialReportResponse() {
    }

    public FinancialReportResponse(ReportSummaryResponse summary,
                                   List<CategoryReportResponse> categoryBreakdown,
                                   List<PaymentMethodReportResponse> paymentMethodBreakdown,
                                   BudgetComparisonResponse budgetComparison,
                                   List<ExpenseReportItemResponse> expenses) {
        this.summary = summary;
        this.categoryBreakdown = categoryBreakdown;
        this.paymentMethodBreakdown = paymentMethodBreakdown;
        this.budgetComparison = budgetComparison;
        this.expenses = expenses;
    }

    public ReportSummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(ReportSummaryResponse summary) {
        this.summary = summary;
    }

    public List<CategoryReportResponse> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryReportResponse> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<PaymentMethodReportResponse> getPaymentMethodBreakdown() {
        return paymentMethodBreakdown;
    }

    public void setPaymentMethodBreakdown(List<PaymentMethodReportResponse> paymentMethodBreakdown) {
        this.paymentMethodBreakdown = paymentMethodBreakdown;
    }

    public BudgetComparisonResponse getBudgetComparison() {
        return budgetComparison;
    }

    public void setBudgetComparison(BudgetComparisonResponse budgetComparison) {
        this.budgetComparison = budgetComparison;
    }

    public List<ExpenseReportItemResponse> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseReportItemResponse> expenses) {
        this.expenses = expenses;
    }
}
