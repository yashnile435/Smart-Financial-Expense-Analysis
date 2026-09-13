package com.smartfinancialexpenseanalysis.dto;

import java.math.BigDecimal;

/**
 * Alert severity levels based on budget utilization percentage.
 */
public enum BudgetAlertLevel {
    NORMAL,
    WARNING,
    CRITICAL_WARNING,
    EXCEEDED;

    private static final BigDecimal EIGHTY = new BigDecimal("80.00");
    private static final BigDecimal NINETY = new BigDecimal("90.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /**
     * Maps utilization percentage to the appropriate alert level.
     * 0 - 79.99%: NORMAL
     * 80 - 89.99%: WARNING
     * 90 - 99.99%: CRITICAL_WARNING
     * 100%+: EXCEEDED
     */
    public static BudgetAlertLevel fromUtilization(BigDecimal utilizationPercentage) {
        if (utilizationPercentage == null || utilizationPercentage.compareTo(EIGHTY) < 0) {
            return NORMAL;
        } else if (utilizationPercentage.compareTo(NINETY) < 0) {
            return WARNING;
        } else if (utilizationPercentage.compareTo(HUNDRED) < 0) {
            return CRITICAL_WARNING;
        } else {
            return EXCEEDED;
        }
    }
}
