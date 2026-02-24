package com.smart.mobility.smartmobilitybillingservice.messaging;

import com.smart.mobility.smartmobilitybillingservice.config.RabbitMQConfig;
import com.smart.mobility.smartmobilitybillingservice.dto.TripPricedEvent;
import com.smart.mobility.smartmobilitybillingservice.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripPricedConsumer {

    private final BillingService billingService;

    /**
     * Listens to the trip-priced-queue and delegates processing to BillingService.
     * On failure, messages will be sent to the DLQ after exhausting retries
     * (configured in properties).
     */
    @RabbitListener(queues = RabbitMQConfig.TRIP_PRICED_QUEUE)
    public void onTripPriced(TripPricedEvent event) {
        log.info("Received TRIP_PRICED event: tripId={}, userId={}, amount={}",
                event.tripId(), event.userId(), event.finalAmount());
        billingService.processDebit(event);
    }
}
