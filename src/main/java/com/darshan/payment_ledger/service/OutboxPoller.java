package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.entity.OutboxEvent;
import com.darshan.payment_ledger.enums.OutboxStatus;
import com.darshan.payment_ledger.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    // Per-send bound so a single publish never hangs the scheduled cycle.
    private static final long SEND_TIMEOUT_SECONDS = 5;

    // Feature flag. When no Kafka broker is provisioned (e.g. the free-tier
    // deployment), set KAFKA_ENABLED=false so the poller skips cleanly instead
    // of throwing TimeoutExceptions every cycle. Events stay PENDING and will
    // be relayed the moment a broker is available and the flag is flipped on.
    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    // Runs every 10 seconds after an initial 30-second delay.
    // Reads PENDING outbox rows, publishes them to Kafka, marks them PUBLISHED.
    //
    // Resilience contract: a broker outage must NEVER crash this job or roll back
    // the payment that produced the event. The payment already committed; the
    // event row is durable in the DB. If publishing fails, the event simply stays
    // PENDING and is retried on the next cycle — at-least-once delivery.
    @Scheduled(initialDelay = 30_000, fixedDelay = 10_000)
    @Transactional
    public void pollAndPublish() {
        if (!kafkaEnabled) {
            log.debug("OutboxPoller: Kafka disabled (kafka.enabled=false). Skipping publish cycle.");
            return;
        }

        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            log.debug("OutboxPoller: no pending events.");
            return;
        }

        log.info("OutboxPoller: found {} pending event(s) to publish.", pendingEvents.size());

        int published = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                // Send the JSON payload to the "transaction-events" Kafka topic.
                // Key = aggregateId (referenceId) — ensures all events for the same
                // transaction go to the same partition, preserving order.
                // .get(timeout) bounds the wait so the cycle can't hang on a dead broker.
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Mark as PUBLISHED only after a confirmed send — never optimistically.
                event.setStatus(OutboxStatus.PUBLISHED);
                outboxEventRepository.save(event);
                published++;

                log.info("OutboxPoller: published event for transaction {} to topic '{}'.",
                        event.getAggregateId(), TOPIC);
            } catch (Exception ex) {
                // Broker unreachable / topic missing. Leave this (and the rest) PENDING
                // for the next cycle. Log ONE concise line — no stack trace spam — and
                // stop early, since a down broker will fail the remaining sends identically.
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("OutboxPoller: Kafka unavailable ({}). {} event(s) left PENDING for retry.",
                        ex.getMessage(), pendingEvents.size() - published);
                break;
            }
        }

        if (published > 0) {
            log.info("OutboxPoller: published {} event(s) this cycle.", published);
        }
    }
}