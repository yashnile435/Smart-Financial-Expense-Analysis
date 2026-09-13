package com.smartfinancialexpenseanalysis.entity;

import java.time.LocalDate;

/**
 * Supported recurrence frequencies for scheduled recurring expenses.
 */
public enum RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * Calculates the subsequent occurrence date after the provided baseline date.
     *
     * @param fromDate baseline date
     * @return next occurrence date
     */
    public LocalDate nextDate(LocalDate fromDate) {
        if (fromDate == null) {
            return LocalDate.now();
        }
        return switch (this) {
            case DAILY -> fromDate.plusDays(1);
            case WEEKLY -> fromDate.plusWeeks(1);
            case MONTHLY -> fromDate.plusMonths(1);
            case YEARLY -> fromDate.plusYears(1);
        };
    }
}
