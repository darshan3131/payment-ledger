-- V1: Create accounts table
-- This is the foundation. Every transaction involves two accounts.
-- balance is stored as BIGINT (paise/cents) — never use DECIMAL for money.
-- Floating point arithmetic (0.1 + 0.2 ≠ 0.3) will corrupt financial data.
-- version column is for Hibernate optimistic locking (@Version).
-- Hibernate auto-manages it: reads version, increments on UPDATE, throws if changed.

CREATE TABLE accounts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(255) NOT NULL,
    holder_name   VARCHAR(255) NOT NULL,
    currency      VARCHAR(10)  NOT NULL,
    balance       BIGINT       NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    -- Optimistic locking version counter (managed by Hibernate @Version)
    version       BIGINT       NOT NULL DEFAULT 0,

    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    CONSTRAINT uq_account_number UNIQUE (account_number)
);

-- Index on account_number: every transaction lookup hits this column
CREATE INDEX idx_accounts_account_number ON accounts (account_number);
CREATE INDEX idx_accounts_status ON accounts (status);
