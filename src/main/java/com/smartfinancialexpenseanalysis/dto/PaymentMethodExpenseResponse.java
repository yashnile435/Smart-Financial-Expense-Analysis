package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Payment method spending breakdown DTO for dashboard analytics.
 */
public class PaymentMethodExpenseResponse {

    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal percentage;

    public PaymentMethodExpenseResponse() {
    }

    public PaymentMethodExpenseResponse(PaymentMethod paymentMethod, BigDecimal totalAmount, BigDecimal percentage) {
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
