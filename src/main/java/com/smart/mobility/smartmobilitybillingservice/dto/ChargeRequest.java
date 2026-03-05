package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;

public record ChargeRequest(
        BigDecimal amount,
        String description
) {
}
