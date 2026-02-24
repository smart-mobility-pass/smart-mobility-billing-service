package com.smart.mobility.smartmobilitybillingservice.messaging;

import com.smart.mobility.smartmobilitybillingservice.config.RabbitMQConfig;
import com.smart.mobility.smartmobilitybillingservice.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(String tripId, Long userId, BigDecimal amount) {
        PaymentEvent event = new PaymentEvent(
                tripId, userId, amount, "COMPLETED", null, LocalDateTime.now());
        log.info("Publishing PAYMENT_COMPLETED for tripId={}", tripId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.ROUTING_PAYMENT_COMPLETED,
                event);
    }

    public void publishPaymentFailed(String tripId, Long userId, BigDecimal amount, String reason) {
        PaymentEvent event = new PaymentEvent(
                tripId, userId, amount, "FAILED", reason, LocalDateTime.now());
        log.warn("Publishing PAYMENT_FAILED for tripId={}, reason={}", tripId, reason);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.ROUTING_PAYMENT_FAILED,
                event);
    }
}
