package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a payment is completed or failed.
 */
public record PaymentEvent(
        String tripId,
        Long userId,
        BigDecimal amount,
        String status, // "COMPLETED" or "FAILED"
        String reason, // Populated on failure
        LocalDateTime processedAt) {
}
