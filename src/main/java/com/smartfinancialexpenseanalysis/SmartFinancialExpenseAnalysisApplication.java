package com.smartfinancialexpenseanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Smart Financial Expense Analysis application.
 */
@EnableScheduling
@SpringBootApplication
public class SmartFinancialExpenseAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFinancialExpenseAnalysisApplication.class, args);
    }

}
