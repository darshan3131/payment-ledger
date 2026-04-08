package com.darshan.payment_ledger.enums;

public enum OutboxStatus {
    PENDING,    // written to outbox table, not yet published to Kafka
    PUBLISHED   // successfully published to Kafka
}