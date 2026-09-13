package com.smartfinancialexpenseanalysis.config;

import com.smartfinancialexpenseanalysis.entity.PaymentOption;
import com.smartfinancialexpenseanalysis.repository.PaymentOptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds standard payment options upon application startup if they are not already present.
 * Idempotent, safe, and does not create duplicate entries.
 */
@Component
@Order(2)
public class PaymentOptionDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOptionDataInitializer.class);

    private final PaymentOptionRepository paymentOptionRepository;

    public PaymentOptionDataInitializer(PaymentOptionRepository paymentOptionRepository) {
        this.paymentOptionRepository = paymentOptionRepository;
    }

    @Override
    public void run(String... args) {
        Map<String, String> defaultOptions = new LinkedHashMap<>();
        defaultOptions.put("Cash", "Physical currency and cash transactions");
        defaultOptions.put("UPI", "Instant unified payments interface (GPay, PhonePe, Paytm, etc.)");
        defaultOptions.put("Credit Card", "Major credit card networks (Visa, MasterCard, Amex, RuPay)");
        defaultOptions.put("Debit Card", "Bank-issued debit and ATM cards");
        defaultOptions.put("Net Banking", "Direct online net banking portal transfers");
        defaultOptions.put("Bank Transfer", "NEFT, RTGS, and IMPS wire transfers");
        defaultOptions.put("Other", "Gift cards, vouchers, wallets, and other payment channels");

        defaultOptions.forEach((name, description) -> {
            if (!paymentOptionRepository.existsByNameIgnoreCase(name)) {
                paymentOptionRepository.save(new PaymentOption(name, description, true));
                logger.info("Initialized default payment option: {}", name);
            }
        });
    }
}
