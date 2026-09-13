package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * System-level summary statistics for administrator dashboard.
 */
public class AdminSummaryResponse {

    private long totalUsers;
    private long activeUsers;
    private long totalExpenses;
    private BigDecimal totalExpenseAmount;
    private BigDecimal currentMonthExpenseAmount;
    private long totalBudgets;
    private BigDecimal totalBudgetAmount;

    public AdminSummaryResponse() {
    }

    public AdminSummaryResponse(long totalUsers, long activeUsers, long totalExpenses,
                                BigDecimal totalExpenseAmount, BigDecimal currentMonthExpenseAmount,
                                long totalBudgets, BigDecimal totalBudgetAmount) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalExpenses = totalExpenses;
        this.totalExpenseAmount = totalExpenseAmount;
        this.currentMonthExpenseAmount = currentMonthExpenseAmount;
        this.totalBudgets = totalBudgets;
        this.totalBudgetAmount = totalBudgetAmount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(long totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getTotalExpenseAmount() {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
    }

    public BigDecimal getCurrentMonthExpenseAmount() {
        return currentMonthExpenseAmount;
    }

    public void setCurrentMonthExpenseAmount(BigDecimal currentMonthExpenseAmount) {
        this.currentMonthExpenseAmount = currentMonthExpenseAmount;
    }

    public long getTotalBudgets() {
        return totalBudgets;
    }

    public void setTotalBudgets(long totalBudgets) {
        this.totalBudgets = totalBudgets;
    }

    public BigDecimal getTotalBudgetAmount() {
        return totalBudgetAmount;
    }

    public void setTotalBudgetAmount(BigDecimal totalBudgetAmount) {
        this.totalBudgetAmount = totalBudgetAmount;
    }
}
