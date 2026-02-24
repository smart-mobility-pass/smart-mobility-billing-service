package com.smart.mobility.smartmobilitybillingservice.dto;

public record CreateAccountRequest(
        Long userId,
        String currency) {
}
