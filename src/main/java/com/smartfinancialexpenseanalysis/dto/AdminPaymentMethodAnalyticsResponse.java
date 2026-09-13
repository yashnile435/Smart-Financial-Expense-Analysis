package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * System-wide payment method breakdown analytics for administrators.
 */
public class AdminPaymentMethodAnalyticsResponse {

    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private long transactionCount;
    private BigDecimal percentage;

    public AdminPaymentMethodAnalyticsResponse() {
    }

    public AdminPaymentMethodAnalyticsResponse(PaymentMethod paymentMethod, BigDecimal totalAmount,
                                               long transactionCount, BigDecimal percentage) {
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
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
