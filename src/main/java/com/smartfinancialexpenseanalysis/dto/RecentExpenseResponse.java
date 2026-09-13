package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recent expense item DTO for dashboard table view.
 * Deliberately excludes user security details.
 */
public class RecentExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private String categoryName;
    private PaymentMethod paymentMethod;
    private LocalDate date;
    private String description;

    public RecentExpenseResponse() {
    }

    public RecentExpenseResponse(Long id, BigDecimal amount, String categoryName,
                                 PaymentMethod paymentMethod, LocalDate date, String description) {
        this.id = id;
        this.amount = amount;
        this.categoryName = categoryName;
        this.paymentMethod = paymentMethod;
        this.date = date;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
