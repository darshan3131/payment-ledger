package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.OutboxEvent;
import com.darshan.payment_ledger.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Used by OutboxPoller every 10 seconds:
    // "give me all events that haven't been sent to Kafka yet"
    List<OutboxEvent> findByStatus(OutboxStatus status);

    // Used for debugging/lookup:
    // "show me the outbox event for transaction TXN22C7E9FE8EB9"
    Optional<OutboxEvent> findByAggregateId(String aggregateId);
}