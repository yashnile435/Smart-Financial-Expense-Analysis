package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.BudgetStatus;

import java.math.BigDecimal;

/**
 * Safe budget details and financial metrics returned in API responses.
 */
public class BudgetResponse {

    private Long id;
    private Integer month;
    private Integer year;
    private BigDecimal budgetAmount;
    private BigDecimal totalExpenses;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;
    private BudgetStatus status;

    public BudgetResponse() {
    }

    public BudgetResponse(Long id, Integer month, Integer year, BigDecimal budgetAmount,
                          BigDecimal totalExpenses, BigDecimal remainingAmount,
                          BigDecimal utilizationPercentage, BudgetStatus status) {
        this.id = id;
        this.month = month;
        this.year = year;
        this.budgetAmount = budgetAmount;
        this.totalExpenses = totalExpenses;
        this.remainingAmount = remainingAmount;
        this.utilizationPercentage = utilizationPercentage;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    /**
     * Alias for budgetAmount to satisfy both budgetAmount and amount field access.
     */
    public BigDecimal getAmount() {
        return budgetAmount;
    }

    public void setAmount(BigDecimal amount) {
        this.budgetAmount = amount;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
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

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }
}
