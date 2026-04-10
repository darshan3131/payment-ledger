-- =============================================================================
-- V1__baseline.sql  —  Schema baseline for PayLedger
-- =============================================================================
--
-- PURPOSE
--   Documents the schema that was previously managed by ddl-auto=update.
--   From this commit forward, ALL schema changes go through versioned Flyway
--   migrations (V2__, V3__, …). Never touch ddl-auto=update again.
--
-- IMPORTANT — baseline-on-migrate=true
--   Because the production database already has these tables, Flyway is
--   configured with spring.flyway.baseline-on-migrate=true.
--   This means:
--     • On an EXISTING database  → Flyway records V1 as "already applied"
--       without executing the SQL. Tables are untouched.
--     • On a FRESH database (CI, new dev box) → This script runs in full,
--       creating the schema from scratch.
--
-- DATABASE: TiDB (MySQL-compatible) — syntax targets MySQL 8 dialect.
-- =============================================================================

-- ─── 1. USERS ────────────────────────────────────────────────────────────────
-- Stores login credentials. Separate from accounts by design:
--   User    = who can log in (username, BCrypt password hash, role)
--   Account = where money lives (account number, balance, currency)
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,              -- BCrypt hash, never plain-text
    role           VARCHAR(50)  NOT NULL,              -- CUSTOMER | BACKOFFICE | ADMIN
    account_number VARCHAR(255),                       -- NULL for BACKOFFICE / ADMIN
    phone          VARCHAR(20),                        -- E.164 format: +91XXXXXXXXXX
    enabled        BIT          NOT NULL DEFAULT 1,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username     (username),
    UNIQUE KEY uk_users_phone        (phone)
);

-- ─── 2. ACCOUNTS ─────────────────────────────────────────────────────────────
-- Financial accounts. Each CUSTOMER user links to exactly one account.
-- Balance stored in smallest currency unit (paise for INR, cents for USD).
-- @Version column (version) enables Hibernate optimistic locking.
CREATE TABLE IF NOT EXISTS accounts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(255) NOT NULL,              -- e.g. ACC9F3B2A1C
    holder_name    VARCHAR(255) NOT NULL,
    user_id        BIGINT,                             -- FK to users.id (NULL for SYSTEM_CASH)
    currency       VARCHAR(10)  NOT NULL,              -- INR | USD | EUR | GBP | …
    balance        BIGINT       NOT NULL,              -- in smallest unit (paise / cents)
    version        BIGINT,                             -- Hibernate optimistic lock counter
    status         VARCHAR(20)  NOT NULL,              -- ACTIVE | INACTIVE | SUSPENDED | FROZEN | CLOSED
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_account_number (account_number)
);

-- ─── 3. TRANSACTIONS ─────────────────────────────────────────────────────────
-- Every money movement. Double-entry: always paired with two ledger_entries.
-- source_account_id / destination_account_id are logical FKs to accounts.id
-- (TiDB supports FKs from v6.6+ but enforces them differently; omitted here
-- to keep the baseline compatible with all TiDB versions).
CREATE TABLE IF NOT EXISTS transactions (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    reference_id            VARCHAR(255) NOT NULL,     -- business-facing ID: TXNxxxxxxxx
    source_account_id       BIGINT       NOT NULL,     -- debit side
    destination_account_id  BIGINT       NOT NULL,     -- credit side
    amount                  BIGINT       NOT NULL,     -- in smallest currency unit
    currency                VARCHAR(10)  NOT NULL,
    type                    VARCHAR(20)  NOT NULL,     -- TRANSFER | PAYMENT | DEPOSIT | WITHDRAWAL | REVERSAL
    status                  VARCHAR(20)  NOT NULL,     -- PENDING | COMPLETED | FAILED | REVERSED
    description             VARCHAR(255),
    idempotency_key         VARCHAR(255),
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_reference_id (reference_id),
    KEY idx_transactions_source      (source_account_id),
    KEY idx_transactions_destination (destination_account_id)
);

-- ─── 4. LEDGER ENTRIES ───────────────────────────────────────────────────────
-- Double-entry bookkeeping. Every transaction produces exactly two rows:
--   one DEBIT  (source account balance decreases)
--   one CREDIT (destination account balance increases)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT       NOT NULL,
    account_id     BIGINT       NOT NULL,
    entry_type     VARCHAR(10)  NOT NULL,              -- DEBIT | CREDIT
    amount         BIGINT       NOT NULL,
    currency       VARCHAR(10)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ledger_transaction (transaction_id),
    KEY idx_ledger_account     (account_id)
);

-- ─── 5. SUPPORT TICKETS ──────────────────────────────────────────────────────
-- Customer-raised issues. Backoffice/Admin resolves and updates status.
CREATE TABLE IF NOT EXISTS support_tickets (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,               -- customer who raised the ticket
    subject      VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL,
    reference_id VARCHAR(255),                        -- optional: linked transaction
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN | IN_PROGRESS | RESOLVED | CLOSED
    resolution   TEXT,                               -- backoffice note
    resolved_by  BIGINT,                             -- backoffice/admin user ID
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_support_user_id (user_id)
);

-- ─── 6. OUTBOX EVENTS ────────────────────────────────────────────────────────
-- Transactional outbox pattern: events are written to this table inside the
-- same DB transaction as the business operation, then a poller publishes them
-- to Kafka. Guarantees at-least-once delivery without distributed transactions.
CREATE TABLE IF NOT EXISTS outbox_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    aggregate_id VARCHAR(255) NOT NULL,               -- referenceId of the triggering transaction
    event_type   VARCHAR(255) NOT NULL,               -- e.g. TRANSACTION_COMPLETED
    payload      TEXT         NOT NULL,               -- JSON blob of event data
    status       VARCHAR(20)  NOT NULL,               -- PENDING | PUBLISHED
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_status (status)                    -- poller queries WHERE status = 'PENDING'
);

-- ─── 7. IDEMPOTENCY KEYS ─────────────────────────────────────────────────────
-- Prevents duplicate transactions on retried requests.
-- Client sends Idempotency-Key header → stored here → duplicate request returns
-- the original response without re-executing the transaction.
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    key_value    VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,               -- referenceId of the transaction it maps to
    created_at   DATETIME(6)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,               -- 24h TTL: cleaned up by scheduled job
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key_value (key_value),
    KEY idx_idempotency_expires_at (expires_at)       -- for expiry cleanup queries
);
