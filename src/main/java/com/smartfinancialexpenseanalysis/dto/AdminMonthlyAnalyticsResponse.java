package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * System-wide monthly trend analytics for administrators.
 */
public class AdminMonthlyAnalyticsResponse {

    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalAmount;
    private long transactionCount;

    public AdminMonthlyAnalyticsResponse() {
    }

    public AdminMonthlyAnalyticsResponse(int year, int month, String monthName,
                                        BigDecimal totalAmount, long transactionCount) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
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
}
