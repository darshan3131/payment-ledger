-- V3: Create ledger_entries table
-- This is the immutable audit trail — the heart of double-entry bookkeeping.
-- Every transaction produces EXACTLY two rows here: one DEBIT, one CREDIT.
-- Rule: sum of all DEBITs must equal sum of all CREDITs across the entire ledger.
-- These rows are NEVER updated or deleted. If something went wrong, you add a
-- reversal entry — you never overwrite history. This is the accounting golden rule.
--
-- entry_type: 'DEBIT' (money leaves account) or 'CREDIT' (money enters account)

CREATE TABLE ledger_entries (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT      NOT NULL,
    account_id     BIGINT      NOT NULL,
    entry_type     VARCHAR(10) NOT NULL,   -- 'DEBIT' or 'CREDIT'
    amount         BIGINT      NOT NULL,
    currency       VARCHAR(10) NOT NULL,
    created_at     DATETIME(6) NOT NULL,

    CONSTRAINT fk_entry_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_entry_account     FOREIGN KEY (account_id)     REFERENCES accounts (id)
);

-- Account statement queries: "show me all ledger entries for account X"
CREATE INDEX idx_ledger_account_id     ON ledger_entries (account_id);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries (transaction_id);
