package com.darshan.payment_ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

// KafkaConsumerService reads events from the "transaction-events" topic.
// OutboxPoller writes to it. This reads from it. The loop is now complete.
//
// WHY A SEPARATE CONSUMER SERVICE?
// Single Responsibility: OutboxPoller owns publishing. This owns consuming.
// In a microservice future, this consumer would live in a separate service
// (e.g. notification-service) that reacts to payment events.
//
// Current job: log + trigger WebhookNotificationService.
// Future: send push notifications, trigger fraud checks, update analytics cache.

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final WebhookNotificationService webhookNotificationService;

    // @KafkaListener binds this method to a Kafka topic.
    // groupId = consumer group — Kafka tracks which messages this group has read.
    // If multiple instances run with the same groupId, Kafka splits partitions between them.
    // Each message is processed by exactly ONE instance — no duplicate processing.
    //
    // topics = "transaction-events" — must match what OutboxPoller publishes to.
    // groupId = "payment-ledger-group" — identifies this app as a consumer group.
    @KafkaListener(topics = "transaction-events", groupId = "payment-ledger-group")
    public void consume(String message) {
        log.info("KafkaConsumer: received event from topic 'transaction-events': {}", message);

        try {
            // Parse the JSON payload and trigger downstream actions.
            // In production this would use ObjectMapper to deserialize to a typed DTO.
            // For now: log it and pass to webhook service.
            webhookNotificationService.handleTransactionEvent(message);
        } catch (Exception e) {
            // Log and continue — never let a consumer crash the listener thread.
            // In production: send to a Dead Letter Queue (DLQ) for manual inspection.
            log.error("KafkaConsumer: failed to process event. Error: {}. Payload: {}", e.getMessage(), message);
        }
    }
}
