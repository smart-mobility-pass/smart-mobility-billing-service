package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountCreditedEvent(
        Long userId,
        BigDecimal amount,
        LocalDateTime timestamp) {
}
