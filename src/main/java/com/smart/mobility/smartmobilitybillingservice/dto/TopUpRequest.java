package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;

public record TopUpRequest(
        BigDecimal amount,
        String description) {
}
