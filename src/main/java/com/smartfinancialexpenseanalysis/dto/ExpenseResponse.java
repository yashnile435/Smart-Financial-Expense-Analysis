package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Safe expense details returned in API responses.
 * Sensitive or unnecessary internal database details are omitted.
 */
public class ExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private CategoryResponse category;
    private LocalDate date;
    private PaymentMethod paymentMethod;
    private String description;

    public ExpenseResponse() {
    }

    public ExpenseResponse(Long id, BigDecimal amount, CategoryResponse category, LocalDate date, PaymentMethod paymentMethod, String description) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.paymentMethod = paymentMethod;
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

    public CategoryResponse getCategory() {
        return category;
    }

    public void setCategory(CategoryResponse category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
