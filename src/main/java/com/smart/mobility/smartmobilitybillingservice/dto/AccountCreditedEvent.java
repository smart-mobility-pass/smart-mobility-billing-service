package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountCreditedEvent(
                String userId,
                BigDecimal amount,
                LocalDateTime timestamp) {
}
