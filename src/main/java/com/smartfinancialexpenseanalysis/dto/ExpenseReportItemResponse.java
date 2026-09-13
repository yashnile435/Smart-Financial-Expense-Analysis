package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Individual expense item formatted for financial reports.
 */
public class ExpenseReportItemResponse {

    private Long id;
    private LocalDate date;
    private String category;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String description;

    public ExpenseReportItemResponse() {
    }

    public ExpenseReportItemResponse(Long id, LocalDate date, String category,
                                     BigDecimal amount, PaymentMethod paymentMethod,
                                     String description) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
