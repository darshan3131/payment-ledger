package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.entity.OutboxEvent;
import com.darshan.payment_ledger.enums.OutboxStatus;
import com.darshan.payment_ledger.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    // Runs every 10 seconds after an initial 30-second delay.
    // Reads PENDING outbox rows, publishes them to Kafka, marks them PUBLISHED.
    @Scheduled(initialDelay = 30_000, fixedDelay = 10_000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            log.debug("OutboxPoller: no pending events.");
            return;
        }

        log.info("OutboxPoller: found {} pending event(s) to publish.", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            // Send the JSON payload to the "transaction-events" Kafka topic.
            // Key = aggregateId (referenceId) — ensures all events for the same
            // transaction go to the same partition, preserving order.
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload());

            // Mark as PUBLISHED so we don't re-send on the next poll.
            event.setStatus(OutboxStatus.PUBLISHED);
            outboxEventRepository.save(event);

            log.info("OutboxPoller: published event for transaction {} to topic '{}'.",
                    event.getAggregateId(), TOPIC);
        }
    }
}