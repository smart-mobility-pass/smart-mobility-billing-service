package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal dailySpent,
        String currency) {
}
