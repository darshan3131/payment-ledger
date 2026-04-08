package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// WHY A SEPARATE DTO?
// We never expose JPA entities directly to the API.
// Reason 1: entities have @ManyToOne / @OneToMany relationships — Jackson
//            would try to serialize them recursively → infinite loop or
//            unwanted data leaking out.
// Reason 2: the entity shape is driven by DB design; the DTO shape is driven
//            by what the frontend actually needs. These two concerns should
//            be separate.
// Reason 3: if you change the DB schema, the DTO stays stable → no breaking
//            changes in the API contract.

@Data
@Builder
public class LedgerEntryResponse {

    private Long id;

    // Which transaction caused this entry?
    // Frontend uses this to cross-reference the full transaction details.
    private String referenceId;       // transaction reference (e.g. TXN1A2B3C4D5E6F)
    private String transactionType;   // TRANSFER, PAYMENT, etc.

    // DEBIT or CREDIT — the core of double-entry bookkeeping.
    // DEBIT  = money LEFT this account  (source account perspective)
    // CREDIT = money ENTERED this account (destination account perspective)
    private String entryType;

    private Long amount;
    private String formattedAmount;   // e.g. "₹500.00" — ready for display
    private Currency currency;

    // Which other account was on the other side?
    // Lets the customer see "Sent to ACC123..." or "Received from ACC456..."
    private String counterpartyAccountNumber;

    private String description;       // from the transaction

    private LocalDateTime createdAt;
}
