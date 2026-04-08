-- V4: Create idempotency_keys table
-- Purpose: prevent duplicate money movement when a client retries a request.
-- Scenario: client sends POST /transactions, network times out, client retries.
-- Without idempotency: two transactions happen, money moves twice.
-- With idempotency: second request hits this table, finds the key, returns the
-- original transaction response — no money moved a second time.
--
-- key_value: the UUID the client sends in the request header/body.
-- reference_id: the TXNxxxxxxxx we generated for the first successful request.
-- expires_at: keys are valid for 24 hours (set in IdempotencyKey @PrePersist).
-- After expiry, the same key could theoretically be reused for a different transaction.

CREATE TABLE idempotency_keys (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_value    VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,

    CONSTRAINT uq_idempotency_key_value UNIQUE (key_value)
);

-- Every incoming transaction does a lookup by key_value — must be fast
CREATE INDEX idx_idempotency_key_value   ON idempotency_keys (key_value);
CREATE INDEX idx_idempotency_expires_at  ON idempotency_keys (expires_at);
