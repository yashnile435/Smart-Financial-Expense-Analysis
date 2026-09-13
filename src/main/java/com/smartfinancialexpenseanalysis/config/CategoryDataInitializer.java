package com.smartfinancialexpenseanalysis.config;

import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds standard categories upon application startup if they are not already present.
 * Idempotent, safe, and does not create duplicate entries or test credentials.
 */
@Component
public class CategoryDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CategoryDataInitializer.class);

    private final CategoryRepository categoryRepository;

    public CategoryDataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        Map<String, String> defaultCategories = new LinkedHashMap<>();
        defaultCategories.put("Food", "Groceries, dining out, restaurants, and food delivery");
        defaultCategories.put("Travel", "Flights, trains, fuel, taxis, and public transit");
        defaultCategories.put("Shopping", "Clothing, electronics, household goods, and personal items");
        defaultCategories.put("Bills", "Electricity, water, internet, phone, and utility bills");
        defaultCategories.put("Education", "Tuition, courses, books, and learning resources");
        defaultCategories.put("Entertainment", "Movies, streaming subscriptions, games, and events");
        defaultCategories.put("Health", "Medical expenses, pharmacy, insurance, and fitness");
        defaultCategories.put("Rent", "Monthly apartment or house rent and lease payments");
        defaultCategories.put("Utilities", "Gas, water, power, maintenance, and municipal services");
        defaultCategories.put("Other", "Miscellaneous and uncategorized expenses");

        defaultCategories.forEach((name, description) -> {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(new Category(name, description));
                logger.info("Initialized default category: {}", name);
            }
        });
    }
}
