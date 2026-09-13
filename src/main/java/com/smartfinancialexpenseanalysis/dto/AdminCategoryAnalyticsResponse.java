package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * System-wide category breakdown analytics for administrators.
 */
public class AdminCategoryAnalyticsResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private long transactionCount;
    private BigDecimal percentage;

    public AdminCategoryAnalyticsResponse() {
    }

    public AdminCategoryAnalyticsResponse(Long categoryId, String categoryName, BigDecimal totalAmount,
                                          long transactionCount, BigDecimal percentage) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.percentage = percentage;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
