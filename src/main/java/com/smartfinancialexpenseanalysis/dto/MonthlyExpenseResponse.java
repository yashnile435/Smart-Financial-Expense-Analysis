package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Monthly trend spending point DTO for continuous 6-month chart rendering.
 */
public class MonthlyExpenseResponse {

    private Integer month;
    private Integer year;
    private BigDecimal totalAmount;

    public MonthlyExpenseResponse() {
    }

    public MonthlyExpenseResponse(Integer month, Integer year, BigDecimal totalAmount) {
        this.month = month;
        this.year = year;
        this.totalAmount = totalAmount;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
