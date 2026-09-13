package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * High-level and category-breakdown spending comparison between two periods.
 */
public class SpendingComparisonResponse {

    private Integer currentMonth;
    private Integer currentYear;
    private Integer comparisonMonth;
    private Integer comparisonYear;
    private BigDecimal currentPeriodTotal;
    private BigDecimal comparisonPeriodTotal;
    private BigDecimal difference;
    private BigDecimal percentageChange;
    private ComparisonDirection direction;
    private BigDecimal monthlyAverage;
    private BigDecimal currentVsAverageDifference;
    private List<CategoryComparisonResponse> categoryComparisons;

    public SpendingComparisonResponse() {
    }

    public SpendingComparisonResponse(Integer currentMonth, Integer currentYear,
                                      Integer comparisonMonth, Integer comparisonYear,
                                      BigDecimal currentPeriodTotal, BigDecimal comparisonPeriodTotal,
                                      BigDecimal difference, BigDecimal percentageChange,
                                      ComparisonDirection direction, BigDecimal monthlyAverage,
                                      BigDecimal currentVsAverageDifference,
                                      List<CategoryComparisonResponse> categoryComparisons) {
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
        this.comparisonMonth = comparisonMonth;
        this.comparisonYear = comparisonYear;
        this.currentPeriodTotal = currentPeriodTotal;
        this.comparisonPeriodTotal = comparisonPeriodTotal;
        this.difference = difference;
        this.percentageChange = percentageChange;
        this.direction = direction;
        this.monthlyAverage = monthlyAverage;
        this.currentVsAverageDifference = currentVsAverageDifference;
        this.categoryComparisons = categoryComparisons;
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

    public Integer getComparisonMonth() {
        return comparisonMonth;
    }

    public void setComparisonMonth(Integer comparisonMonth) {
        this.comparisonMonth = comparisonMonth;
    }

    public Integer getComparisonYear() {
        return comparisonYear;
    }

    public void setComparisonYear(Integer comparisonYear) {
        this.comparisonYear = comparisonYear;
    }

    public BigDecimal getCurrentPeriodTotal() {
        return currentPeriodTotal;
    }

    public void setCurrentPeriodTotal(BigDecimal currentPeriodTotal) {
        this.currentPeriodTotal = currentPeriodTotal;
    }

    public BigDecimal getComparisonPeriodTotal() {
        return comparisonPeriodTotal;
    }

    public void setComparisonPeriodTotal(BigDecimal comparisonPeriodTotal) {
        this.comparisonPeriodTotal = comparisonPeriodTotal;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(BigDecimal percentageChange) {
        this.percentageChange = percentageChange;
    }

    public ComparisonDirection getDirection() {
        return direction;
    }

    public void setDirection(ComparisonDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getMonthlyAverage() {
        return monthlyAverage;
    }

    public void setMonthlyAverage(BigDecimal monthlyAverage) {
        this.monthlyAverage = monthlyAverage;
    }

    public BigDecimal getCurrentVsAverageDifference() {
        return currentVsAverageDifference;
    }

    public void setCurrentVsAverageDifference(BigDecimal currentVsAverageDifference) {
        this.currentVsAverageDifference = currentVsAverageDifference;
    }

    public List<CategoryComparisonResponse> getCategoryComparisons() {
        return categoryComparisons;
    }

    public void setCategoryComparisons(List<CategoryComparisonResponse> categoryComparisons) {
        this.categoryComparisons = categoryComparisons;
    }
}
