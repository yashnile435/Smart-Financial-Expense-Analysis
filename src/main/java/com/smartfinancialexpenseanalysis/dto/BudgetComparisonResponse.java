package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Budget vs actual spending comparison response for financial reports.
 */
public class BudgetComparisonResponse {

    private Integer month;
    private Integer year;
    private boolean budgetConfigured;
    private BigDecimal budgetAmount;
    private BigDecimal actualExpense;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;
    private String status;

    public BudgetComparisonResponse() {
    }

    public BudgetComparisonResponse(Integer month, Integer year, boolean budgetConfigured,
                                    BigDecimal budgetAmount, BigDecimal actualExpense,
                                    BigDecimal remainingAmount, BigDecimal utilizationPercentage,
                                    String status) {
        this.month = month;
        this.year = year;
        this.budgetConfigured = budgetConfigured;
        this.budgetAmount = budgetAmount;
        this.actualExpense = actualExpense;
        this.remainingAmount = remainingAmount;
        this.utilizationPercentage = utilizationPercentage;
        this.status = status;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public boolean isBudgetConfigured() {
        return budgetConfigured;
    }

    public void setBudgetConfigured(boolean budgetConfigured) {
        this.budgetConfigured = budgetConfigured;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getActualExpense() {
        return actualExpense;
    }

    public void setActualExpense(BigDecimal actualExpense) {
        this.actualExpense = actualExpense;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(BigDecimal utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
