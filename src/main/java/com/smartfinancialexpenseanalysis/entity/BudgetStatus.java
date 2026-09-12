package com.smartfinancialexpenseanalysis.entity;

import java.math.BigDecimal;

/**
 * Represents the health status of a budget based on utilization percentage.
 * 
 * Rules:
 * - Under 80%: UNDER_BUDGET
 * - 80% through 100%: NEAR_LIMIT
 * - Above 100%: OVER_BUDGET
 */
public enum BudgetStatus {
    UNDER_BUDGET,
    NEAR_LIMIT,
    OVER_BUDGET;

    private static final BigDecimal EIGHTY = new BigDecimal("80.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /**
     * Determines status from the calculated utilization percentage.
     *
     * @param utilizationPercentage utilization percentage
     * @return BudgetStatus enum constant
     */
    public static BudgetStatus fromUtilization(BigDecimal utilizationPercentage) {
        if (utilizationPercentage == null) {
            return UNDER_BUDGET;
        }
        if (utilizationPercentage.compareTo(EIGHTY) < 0) {
            return UNDER_BUDGET;
        } else if (utilizationPercentage.compareTo(HUNDRED) <= 0) {
            return NEAR_LIMIT;
        } else {
            return OVER_BUDGET;
        }
    }
}
