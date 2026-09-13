package com.smartfinancialexpenseanalysis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.Objects;

/**
 * Extensible value object representing a payment method.
 * Maintains 100% backward compatibility with previous enum usage (CASH, UPI, CARD, etc.)
 * while allowing administrators to define dynamic payment options in the database without code changes.
 */
public class PaymentMethod implements Serializable, Comparable<PaymentMethod> {

    private static final long serialVersionUID = 1L;

    public static final PaymentMethod CASH = new PaymentMethod("CASH");
    public static final PaymentMethod UPI = new PaymentMethod("UPI");
    public static final PaymentMethod CARD = new PaymentMethod("CARD");
    public static final PaymentMethod BANK_TRANSFER = new PaymentMethod("BANK_TRANSFER");
    public static final PaymentMethod OTHER = new PaymentMethod("OTHER");

    private final String value;

    public PaymentMethod() {
        this.value = "OTHER";
    }

    public PaymentMethod(String value) {
        this.value = value != null ? value.trim() : "OTHER";
    }

    @JsonCreator
    public static PaymentMethod of(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        return switch (trimmed.toUpperCase()) {
            case "CASH" -> CASH;
            case "UPI" -> UPI;
            case "CARD" -> CARD;
            case "BANK_TRANSFER" -> BANK_TRANSFER;
            case "OTHER" -> OTHER;
            default -> new PaymentMethod(trimmed);
        };
    }

    public static PaymentMethod valueOf(String name) {
        return of(name);
    }

    public static PaymentMethod[] values() {
        return new PaymentMethod[]{CASH, UPI, CARD, BANK_TRANSFER, OTHER};
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String name() {
        return value;
    }

    public int ordinal() {
        return switch (value.toUpperCase()) {
            case "CASH" -> 0;
            case "UPI" -> 1;
            case "CARD" -> 2;
            case "BANK_TRANSFER" -> 3;
            case "OTHER" -> 4;
            default -> 5;
        };
    }

    @Override
    public int compareTo(PaymentMethod o) {
        if (o == null) return 1;
        return this.value.compareToIgnoreCase(o.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof PaymentMethod that) {
            return this.value.equalsIgnoreCase(that.value);
        }
        if (o instanceof String str) {
            return this.value.equalsIgnoreCase(str);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
