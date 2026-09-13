package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.PaymentOptionResponse;
import com.smartfinancialexpenseanalysis.repository.PaymentOptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving available/active payment options.
 * Accessible to authenticated users and public form consumers.
 */
@RestController
@RequestMapping("/api/payment-options")
public class PaymentOptionController {

    private final PaymentOptionRepository paymentOptionRepository;

    public PaymentOptionController(PaymentOptionRepository paymentOptionRepository) {
        this.paymentOptionRepository = paymentOptionRepository;
    }

    /**
     * Retrieves all active payment options for expense entry.
     *
     * @return List of active PaymentOptionResponse DTOs sorted by name
     */
    @GetMapping
    public ResponseEntity<List<PaymentOptionResponse>> getActivePaymentOptions() {
        List<PaymentOptionResponse> options = paymentOptionRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(po -> new PaymentOptionResponse(
                        po.getId(),
                        po.getName(),
                        po.getDescription(),
                        po.isActive(),
                        po.getCreatedAt(),
                        po.getUpdatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(options);
    }
}
