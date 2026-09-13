package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Payment method financial report breakdown item.
 */
public class PaymentMethodReportResponse {

    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
    private long expenseCount;

    public PaymentMethodReportResponse() {
    }

    public PaymentMethodReportResponse(PaymentMethod paymentMethod, BigDecimal totalAmount,
                                       BigDecimal percentage, long expenseCount) {
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
        this.expenseCount = expenseCount;
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

    public long getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(long expenseCount) {
        this.expenseCount = expenseCount;
    }
}
