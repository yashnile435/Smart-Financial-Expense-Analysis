package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * System-wide budget overview statistics for administrators.
 */
public class AdminBudgetSummaryResponse {

    private long totalBudgets;
    private BigDecimal totalBudgetAmount;
    private BigDecimal averageBudgetAmount;
    private long budgetsUnderBudget;
    private long budgetsNearLimit;
    private long budgetsOverBudget;

    public AdminBudgetSummaryResponse() {
    }

    public AdminBudgetSummaryResponse(long totalBudgets, BigDecimal totalBudgetAmount,
                                      BigDecimal averageBudgetAmount, long budgetsUnderBudget,
                                      long budgetsNearLimit, long budgetsOverBudget) {
        this.totalBudgets = totalBudgets;
        this.totalBudgetAmount = totalBudgetAmount;
        this.averageBudgetAmount = averageBudgetAmount;
        this.budgetsUnderBudget = budgetsUnderBudget;
        this.budgetsNearLimit = budgetsNearLimit;
        this.budgetsOverBudget = budgetsOverBudget;
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

    public BigDecimal getAverageBudgetAmount() {
        return averageBudgetAmount;
    }

    public void setAverageBudgetAmount(BigDecimal averageBudgetAmount) {
        this.averageBudgetAmount = averageBudgetAmount;
    }

    public long getBudgetsUnderBudget() {
        return budgetsUnderBudget;
    }

    public void setBudgetsUnderBudget(long budgetsUnderBudget) {
        this.budgetsUnderBudget = budgetsUnderBudget;
    }

    public long getBudgetsNearLimit() {
        return budgetsNearLimit;
    }

    public void setBudgetsNearLimit(long budgetsNearLimit) {
        this.budgetsNearLimit = budgetsNearLimit;
    }

    public long getBudgetsOverBudget() {
        return budgetsOverBudget;
    }

    public void setBudgetsOverBudget(long budgetsOverBudget) {
        this.budgetsOverBudget = budgetsOverBudget;
    }
}
