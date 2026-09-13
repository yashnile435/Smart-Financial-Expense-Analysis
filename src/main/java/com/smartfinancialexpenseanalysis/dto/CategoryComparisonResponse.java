package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Category-level spending comparison between two periods.
 */
public class CategoryComparisonResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal currentAmount;
    private BigDecimal comparisonAmount;
    private BigDecimal difference;
    private BigDecimal percentageChange;
    private ComparisonDirection direction;

    public CategoryComparisonResponse() {
    }

    public CategoryComparisonResponse(Long categoryId, String categoryName, BigDecimal currentAmount,
                                      BigDecimal comparisonAmount, BigDecimal difference,
                                      BigDecimal percentageChange, ComparisonDirection direction) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.currentAmount = currentAmount;
        this.comparisonAmount = comparisonAmount;
        this.difference = difference;
        this.percentageChange = percentageChange;
        this.direction = direction;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public BigDecimal getComparisonAmount() {
        return comparisonAmount;
    }

    public void setComparisonAmount(BigDecimal comparisonAmount) {
        this.comparisonAmount = comparisonAmount;
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
}
