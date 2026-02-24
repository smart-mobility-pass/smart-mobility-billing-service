package com.smart.mobility.smartmobilitybillingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Exchange ────────────────────────────────────────────────────────────
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // ─── Queues ──────────────────────────────────────────────────────────────
    public static final String TRIP_PRICED_QUEUE = "trip-priced-queue";
    public static final String TRIP_PRICED_DLQ = "trip-priced-queue.dlq";

    // ─── Routing keys ────────────────────────────────────────────────────────
    public static final String ROUTING_TRIP_PRICED = "trip.priced";
    public static final String ROUTING_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_PAYMENT_FAILED = "payment.failed";
    public static final String ROUTING_DLQ = "trip.priced.dlq";

    // ─── Exchange Bean ───────────────────────────────────────────────────────
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    // ─── Dead Letter Queue & Binding ─────────────────────────────────────────
    @Bean
    public Queue tripPricedDlq() {
        return QueueBuilder.durable(TRIP_PRICED_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(tripPricedDlq())
                .to(paymentExchange())
                .with(ROUTING_DLQ);
    }

    // ─── Main Queue (with DLQ config) ────────────────────────────────────────
    @Bean
    public Queue tripPricedQueue() {
        return QueueBuilder.durable(TRIP_PRICED_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Binding tripPricedBinding() {
        return BindingBuilder.bind(tripPricedQueue())
                .to(paymentExchange())
                .with(ROUTING_TRIP_PRICED);
    }

    // ─── JSON Message Converter ───────────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
