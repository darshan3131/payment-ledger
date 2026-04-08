package com.darshan.payment_ledger.entity;

import com.darshan.payment_ledger.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // referenceId of the transaction that triggered this event (e.g. TXN22C7E9FE8EB9)
    @Column(nullable = false)
    private String aggregateId;

    // Describes what happened — "TRANSACTION_COMPLETED"
    // String (not enum) so new event types can be added without a DB migration
    @Column(nullable = false)
    private String eventType;

    // Full event as JSON string — stored as TEXT because JSON can exceed VARCHAR(255)
    // Example: {"referenceId":"TXN123","amount":10000,"currency":"INR","status":"COMPLETED"}
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // PENDING = not yet sent to Kafka
    // PUBLISHED = successfully sent to Kafka
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}