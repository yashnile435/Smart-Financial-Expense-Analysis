package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * System-wide expense overview statistics for administrators.
 */
public class AdminExpenseSummaryResponse {

    private BigDecimal totalExpenseAmount;
    private long expenseCount;
    private BigDecimal averageExpense;
    private BigDecimal highestExpense;
    private BigDecimal currentMonthExpenseAmount;

    public AdminExpenseSummaryResponse() {
    }

    public AdminExpenseSummaryResponse(BigDecimal totalExpenseAmount, long expenseCount,
                                       BigDecimal averageExpense, BigDecimal highestExpense,
                                       BigDecimal currentMonthExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
        this.expenseCount = expenseCount;
        this.averageExpense = averageExpense;
        this.highestExpense = highestExpense;
        this.currentMonthExpenseAmount = currentMonthExpenseAmount;
    }

    public BigDecimal getTotalExpenseAmount() {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
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

    public BigDecimal getCurrentMonthExpenseAmount() {
        return currentMonthExpenseAmount;
    }

    public void setCurrentMonthExpenseAmount(BigDecimal currentMonthExpenseAmount) {
        this.currentMonthExpenseAmount = currentMonthExpenseAmount;
    }
}
