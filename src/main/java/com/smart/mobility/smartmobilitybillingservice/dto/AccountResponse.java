package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;

public record AccountResponse(
                Long id,
                String userId,
                BigDecimal balance,
                BigDecimal dailySpent,
                String currency) {
}
