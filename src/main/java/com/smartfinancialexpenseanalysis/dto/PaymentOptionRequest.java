package com.smartfinancialexpenseanalysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating and updating payment options.
 */
public class PaymentOptionRequest {

    @NotBlank(message = "Payment option name cannot be blank")
    @Size(max = 100, message = "Payment option name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Boolean active = true;

    public PaymentOptionRequest() {
    }

    public PaymentOptionRequest(String name, String description) {
        this.name = name;
        this.description = description;
        this.active = true;
    }

    public PaymentOptionRequest(String name, String description, Boolean active) {
        this.name = name;
        this.description = description;
        this.active = active != null ? active : true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
