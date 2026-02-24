package com.smart.mobility.smartmobilitybillingservice.dto;

import java.math.BigDecimal;

/**
 * Payload received from the TRIP_PRICED RabbitMQ event.
 */
public record TripPricedEvent(
        String tripId,
        Long userId,
        BigDecimal finalAmount) {
}
