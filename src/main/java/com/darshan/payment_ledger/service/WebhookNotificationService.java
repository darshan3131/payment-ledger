package com.darshan.payment_ledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// WebhookNotificationService handles downstream actions after a transaction event.
// Called by KafkaConsumerService after consuming from "transaction-events" topic.
// In production this would HTTP POST to merchant webhook URLs.

@Service
@Slf4j
public class WebhookNotificationService {

    public void handleTransactionEvent(String payload) {
        log.info("WebhookNotificationService: processing event → {}", payload);

        // Production implementation would:
        // 1. Parse JSON payload (ObjectMapper.readValue)
        // 2. Look up merchant webhook URL from DB by sourceAccount or destinationAccount
        // 3. HTTP POST payload to that URL (RestTemplate / WebClient)
        // 4. Retry on failure with exponential backoff (Spring Retry)
        // 5. Log delivery status to webhook_deliveries table

        // For now: log proves the full pipeline works end to end:
        // Transaction → DB → OutboxPoller → Kafka → KafkaConsumer → WebhookService
        log.info("WebhookNotificationService: event processed successfully");
    }
}
