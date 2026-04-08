package com.darshan.payment_ledger.enums;

// WHY THESE TYPES?
// A payment ledger needs to distinguish WHY money moved, not just that it moved.
// TRANSFER   = peer-to-peer (Darshan sends ₹500 to Ravi)
// PAYMENT    = settling a bill or invoice (pay merchant, pay subscription)
// DEPOSIT    = money coming IN from outside the system (bank top-up)
// WITHDRAWAL = money going OUT to the real world (bank payout)
// REVERSAL   = undoing a previous transaction (refund/chargeback)
// Each type affects how the ledger entry is described in statements.

public enum TransactionType {
    TRANSFER,
    PAYMENT,
    DEPOSIT,
    WITHDRAWAL,
    REVERSAL
}
