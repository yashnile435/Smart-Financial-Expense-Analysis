package com.smartfinancialexpenseanalysis.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter automatically translating PaymentMethod value objects
 * to/from database VARCHAR columns.
 */
@Converter(autoApply = true)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        return dbData != null ? PaymentMethod.of(dbData) : null;
    }
}
