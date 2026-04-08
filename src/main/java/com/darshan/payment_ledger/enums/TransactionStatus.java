package com.darshan.payment_ledger.enums;

public enum TransactionStatus {

    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}

        /* ## What Each Status Means

| Status | Meaning |
        |---|---|
        | **PENDING** | Transaction created, processing in progress |
        | **COMPLETED** | Both ledger entries written, money moved successfully |
        | **FAILED** | Something went wrong, no money moved |
        | **REVERSED** | Transaction was completed but later reversed (refund) |

        ---

        ## The Lifecycle of One Transaction
```
Request comes in
      ↓
PENDING ← created here
      ↓
              (ledger entries written)
        ↓
COMPLETED ← success
      ↓
REVERSED ← if refund requested later

        OR

PENDING → FAILED ← if anything goes wrong */