package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.LedgerEntryResponse;
import com.darshan.payment_ledger.service.LedgerEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// WHY THIS CONTROLLER EXISTS:
// This is the heart of a payment ledger system.
// Every other feature (accounts, transactions) is infrastructure.
// The LEDGER is the product — an immutable, auditable record of every
// money movement. Banks call this a "bank statement".
//
// API Design decision: why /accounts/{id}/ledger and not /ledger?
// Because ledger entries are always scoped to an account.
// A customer never asks "show me all ledger entries globally".
// They ask "show me MY statement". The account ID is the natural parent.
// This is called a "sub-resource" pattern in REST API design.

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LedgerEntryController {

    private final LedgerEntryService ledgerEntryService;

    // GET /api/v1/accounts/{id}/ledger
    // Returns the full account statement (all DEBIT + CREDIT entries), newest first.
    // This is what the Customer portal shows as the account statement page.
    // Example response:
    // [
    //   { entryType: "DEBIT",  amount: 50000, formattedAmount: "₹500.00", counterparty: "ACC123..." },
    //   { entryType: "CREDIT", amount: 20000, formattedAmount: "₹200.00", counterparty: "ACC456..." }
    // ]
    @GetMapping("/accounts/{id}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getAccountStatement(
            @PathVariable Long id) {
        return ResponseEntity.ok(ledgerEntryService.getStatementForAccount(id));
    }

    // GET /api/v1/transactions/{transactionId}/ledger
    // Returns the 2 ledger entries for a specific transaction.
    // Useful for the Back Office to verify double-entry was correctly applied:
    // - exactly 1 DEBIT + 1 CREDIT
    // - amounts match
    // - currencies match
    // If a transaction shows only 1 entry, something went wrong mid-flight.
    @GetMapping("/transactions/{transactionId}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getTransactionEntries(
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(ledgerEntryService.getEntriesForTransaction(transactionId));
    }
}
