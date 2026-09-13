package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete financial dashboard summary and analytics response DTO.
 * Deliberately excludes user authentication and internal security details.
 */
public class DashboardResponse {

    private Integer currentMonth;
    private Integer currentYear;
    private BigDecimal totalExpenses;
    private BigDecimal currentMonthExpenses;
    private long expenseCount;
    private long currentMonthExpenseCount;
    private BigDecimal averageExpense;
    private BigDecimal highestExpense;

    // Current Month Budget metrics (safe null when not configured)
    private BigDecimal currentMonthBudget;
    private BigDecimal currentMonthRemaining;
    private BigDecimal currentMonthUtilization;
    private String currentMonthBudgetStatus;

    // Visual & Trend Breakdowns
    private List<CategoryExpenseResponse> categoryBreakdown;
    private List<PaymentMethodExpenseResponse> paymentMethodBreakdown;
    private List<MonthlyExpenseResponse> monthlyTrend;
    private List<RecentExpenseResponse> recentExpenses;
    private List<InsightResponse> financialInsights;

    public DashboardResponse() {
    }

    public DashboardResponse(Integer currentMonth, Integer currentYear, BigDecimal totalExpenses,
                             BigDecimal currentMonthExpenses, long expenseCount, long currentMonthExpenseCount,
                             BigDecimal averageExpense, BigDecimal highestExpense, BigDecimal currentMonthBudget,
                             BigDecimal currentMonthRemaining, BigDecimal currentMonthUtilization,
                             String currentMonthBudgetStatus, List<CategoryExpenseResponse> categoryBreakdown,
                             List<PaymentMethodExpenseResponse> paymentMethodBreakdown,
                             List<MonthlyExpenseResponse> monthlyTrend, List<RecentExpenseResponse> recentExpenses,
                             List<InsightResponse> financialInsights) {
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
        this.totalExpenses = totalExpenses;
        this.currentMonthExpenses = currentMonthExpenses;
        this.expenseCount = expenseCount;
        this.currentMonthExpenseCount = currentMonthExpenseCount;
        this.averageExpense = averageExpense;
        this.highestExpense = highestExpense;
        this.currentMonthBudget = currentMonthBudget;
        this.currentMonthRemaining = currentMonthRemaining;
        this.currentMonthUtilization = currentMonthUtilization;
        this.currentMonthBudgetStatus = currentMonthBudgetStatus;
        this.categoryBreakdown = categoryBreakdown;
        this.paymentMethodBreakdown = paymentMethodBreakdown;
        this.monthlyTrend = monthlyTrend;
        this.recentExpenses = recentExpenses;
        this.financialInsights = financialInsights;
    }

    public Integer getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(Integer currentMonth) {
        this.currentMonth = currentMonth;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getCurrentMonthExpenses() {
        return currentMonthExpenses;
    }

    public void setCurrentMonthExpenses(BigDecimal currentMonthExpenses) {
        this.currentMonthExpenses = currentMonthExpenses;
    }

    public long getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(long expenseCount) {
        this.expenseCount = expenseCount;
    }

    public long getCurrentMonthExpenseCount() {
        return currentMonthExpenseCount;
    }

    public void setCurrentMonthExpenseCount(long currentMonthExpenseCount) {
        this.currentMonthExpenseCount = currentMonthExpenseCount;
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

    public BigDecimal getCurrentMonthBudget() {
        return currentMonthBudget;
    }

    public void setCurrentMonthBudget(BigDecimal currentMonthBudget) {
        this.currentMonthBudget = currentMonthBudget;
    }

    public BigDecimal getCurrentMonthRemaining() {
        return currentMonthRemaining;
    }

    public void setCurrentMonthRemaining(BigDecimal currentMonthRemaining) {
        this.currentMonthRemaining = currentMonthRemaining;
    }

    public BigDecimal getCurrentMonthUtilization() {
        return currentMonthUtilization;
    }

    public void setCurrentMonthUtilization(BigDecimal currentMonthUtilization) {
        this.currentMonthUtilization = currentMonthUtilization;
    }

    public String getCurrentMonthBudgetStatus() {
        return currentMonthBudgetStatus;
    }

    public void setCurrentMonthBudgetStatus(String currentMonthBudgetStatus) {
        this.currentMonthBudgetStatus = currentMonthBudgetStatus;
    }

    public List<CategoryExpenseResponse> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryExpenseResponse> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<PaymentMethodExpenseResponse> getPaymentMethodBreakdown() {
        return paymentMethodBreakdown;
    }

    public void setPaymentMethodBreakdown(List<PaymentMethodExpenseResponse> paymentMethodBreakdown) {
        this.paymentMethodBreakdown = paymentMethodBreakdown;
    }

    public List<MonthlyExpenseResponse> getMonthlyTrend() {
        return monthlyTrend;
    }

    public void setMonthlyTrend(List<MonthlyExpenseResponse> monthlyTrend) {
        this.monthlyTrend = monthlyTrend;
    }

    public List<RecentExpenseResponse> getRecentExpenses() {
        return recentExpenses;
    }

    public void setRecentExpenses(List<RecentExpenseResponse> recentExpenses) {
        this.recentExpenses = recentExpenses;
    }

    public List<InsightResponse> getFinancialInsights() {
        return financialInsights;
    }

    public void setFinancialInsights(List<InsightResponse> financialInsights) {
        this.financialInsights = financialInsights;
    }
}
