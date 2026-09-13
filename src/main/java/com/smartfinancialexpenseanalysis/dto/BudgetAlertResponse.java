package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Budget warning alert data transfer object.
 */
public class BudgetAlertResponse {

    private Long budgetId;
    private Integer month;
    private Integer year;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;
    private BudgetAlertLevel alertLevel;
    private String message;

    public BudgetAlertResponse() {
    }

    public BudgetAlertResponse(Long budgetId, Integer month, Integer year, BigDecimal budgetAmount,
                               BigDecimal spentAmount, BigDecimal remainingAmount,
                               BigDecimal utilizationPercentage, BudgetAlertLevel alertLevel,
                               String message) {
        this.budgetId = budgetId;
        this.month = month;
        this.year = year;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.utilizationPercentage = utilizationPercentage;
        this.alertLevel = alertLevel;
        this.message = message;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
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

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
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

    public BudgetAlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(BudgetAlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
