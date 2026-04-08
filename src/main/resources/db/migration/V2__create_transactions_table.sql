-- V2: Create transactions table
-- A transaction records the intent of a money movement.
-- It starts as PENDING and moves to COMPLETED or FAILED.
-- PENDING-first is intentional: if the app crashes mid-transfer,
-- the ReconciliationService can detect stuck PENDING rows and mark them FAILED.
--
-- idempotency_key: client-supplied unique key to prevent double-processing on retries.
-- reference_id: our internal unique ID for the transaction (TXNxxxxxxxx).
-- source_account_id / destination_account_id: FK to accounts, always required.

CREATE TABLE transactions (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_id           VARCHAR(255) NOT NULL,
    source_account_id      BIGINT       NOT NULL,
    destination_account_id BIGINT       NOT NULL,
    amount                 BIGINT       NOT NULL,
    currency               VARCHAR(10)  NOT NULL,
    type                   VARCHAR(30)  NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    description            VARCHAR(500),
    idempotency_key        VARCHAR(255),
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,

    CONSTRAINT uq_reference_id     UNIQUE (reference_id),
    CONSTRAINT uq_idempotency_key  UNIQUE (idempotency_key),

    CONSTRAINT fk_tx_source      FOREIGN KEY (source_account_id)      REFERENCES accounts (id),
    CONSTRAINT fk_tx_destination FOREIGN KEY (destination_account_id) REFERENCES accounts (id)
);

-- Reconciliation job queries by status + created_at — this index makes it fast
CREATE INDEX idx_transactions_status         ON transactions (status);
CREATE INDEX idx_transactions_status_created ON transactions (status, created_at);
CREATE INDEX idx_transactions_source_account ON transactions (source_account_id);
CREATE INDEX idx_transactions_dest_account   ON transactions (destination_account_id);
