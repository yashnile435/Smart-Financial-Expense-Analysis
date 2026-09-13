package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Advanced financial analytics summary DTO.
 */
public class AdvancedAnalyticsResponse {

    private String highestSpendingCategory;
    private BigDecimal highestSpendingCategoryAmount;
    private String lowestSpendingCategory;
    private BigDecimal lowestSpendingCategoryAmount;
    private BigDecimal averageDailySpending;
    private BigDecimal averageMonthlySpending;
    private BigDecimal largestExpense;
    private long totalExpenseCount;
    private BigDecimal totalExpenseAmount;

    public AdvancedAnalyticsResponse() {
    }

    public AdvancedAnalyticsResponse(String highestSpendingCategory, BigDecimal highestSpendingCategoryAmount,
                                     String lowestSpendingCategory, BigDecimal lowestSpendingCategoryAmount,
                                     BigDecimal averageDailySpending, BigDecimal averageMonthlySpending,
                                     BigDecimal largestExpense, long totalExpenseCount,
                                     BigDecimal totalExpenseAmount) {
        this.highestSpendingCategory = highestSpendingCategory;
        this.highestSpendingCategoryAmount = highestSpendingCategoryAmount;
        this.lowestSpendingCategory = lowestSpendingCategory;
        this.lowestSpendingCategoryAmount = lowestSpendingCategoryAmount;
        this.averageDailySpending = averageDailySpending;
        this.averageMonthlySpending = averageMonthlySpending;
        this.largestExpense = largestExpense;
        this.totalExpenseCount = totalExpenseCount;
        this.totalExpenseAmount = totalExpenseAmount;
    }

    public String getHighestSpendingCategory() {
        return highestSpendingCategory;
    }

    public void setHighestSpendingCategory(String highestSpendingCategory) {
        this.highestSpendingCategory = highestSpendingCategory;
    }

    public BigDecimal getHighestSpendingCategoryAmount() {
        return highestSpendingCategoryAmount;
    }

    public void setHighestSpendingCategoryAmount(BigDecimal highestSpendingCategoryAmount) {
        this.highestSpendingCategoryAmount = highestSpendingCategoryAmount;
    }

    public String getLowestSpendingCategory() {
        return lowestSpendingCategory;
    }

    public void setLowestSpendingCategory(String lowestSpendingCategory) {
        this.lowestSpendingCategory = lowestSpendingCategory;
    }

    public BigDecimal getLowestSpendingCategoryAmount() {
        return lowestSpendingCategoryAmount;
    }

    public void setLowestSpendingCategoryAmount(BigDecimal lowestSpendingCategoryAmount) {
        this.lowestSpendingCategoryAmount = lowestSpendingCategoryAmount;
    }

    public BigDecimal getAverageDailySpending() {
        return averageDailySpending;
    }

    public void setAverageDailySpending(BigDecimal averageDailySpending) {
        this.averageDailySpending = averageDailySpending;
    }

    public BigDecimal getAverageMonthlySpending() {
        return averageMonthlySpending;
    }

    public void setAverageMonthlySpending(BigDecimal averageMonthlySpending) {
        this.averageMonthlySpending = averageMonthlySpending;
    }

    public BigDecimal getLargestExpense() {
        return largestExpense;
    }

    public void setLargestExpense(BigDecimal largestExpense) {
        this.largestExpense = largestExpense;
    }

    public long getTotalExpenseCount() {
        return totalExpenseCount;
    }

    public void setTotalExpenseCount(long totalExpenseCount) {
        this.totalExpenseCount = totalExpenseCount;
    }

    public BigDecimal getTotalExpenseAmount() {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
    }
}
