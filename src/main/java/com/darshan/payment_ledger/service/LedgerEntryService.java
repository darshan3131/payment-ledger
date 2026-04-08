package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.LedgerEntryResponse;
import com.darshan.payment_ledger.entity.LedgerEntry;
import com.darshan.payment_ledger.exception.AccountNotFoundException;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// WHY A DEDICATED SERVICE FOR LEDGER?
// Single Responsibility Principle.
// AccountService knows how to manage accounts.
// TransactionService knows how to process transactions.
// LedgerEntryService knows how to READ the ledger.
// If you put ledger logic inside TransactionService, that class becomes a
// god object — responsible for too many things, hard to test and maintain.

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    // Returns all ledger entries for one account, newest first.
    // This is the account STATEMENT — every DEBIT and CREDIT ever recorded.
    // Interview question: "how do you reconstruct an account's balance?"
    // Answer: start at 0, apply every CREDIT (add) and DEBIT (subtract)
    //         in chronological order. The result = current balance.
    //         That's why ledger entries are immutable — you never edit them.
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getStatementForAccount(Long accountId) {

        // Verify account exists before querying entries
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + accountId));

        return ledgerEntryRepository.findByAccountId(accountId)
                .stream()
                .sorted(Comparator.comparing(LedgerEntry::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Returns both entries for a single transaction.
    // A TRANSFER always produces exactly 2 entries:
    //   1. DEBIT  on the source      (money left)
    //   2. CREDIT on the destination (money arrived)
    // If you see only 1 entry, the transaction was partially applied — a bug.
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getEntriesForTransaction(Long transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private LedgerEntryResponse toResponse(LedgerEntry entry) {

        // The "counterparty" is the OTHER account in the transaction.
        // If this is a DEBIT entry, the counterparty is the destination.
        // If this is a CREDIT entry, the counterparty is the source.
        // This gives the customer: "Sent to ACC123..." or "Received from ACC456..."
        String counterparty;
        String description = entry.getTransaction().getDescription();

        if ("DEBIT".equals(entry.getEntryType())) {
            counterparty = entry.getTransaction().getDestinationAccount().getAccountNumber();
        } else {
            counterparty = entry.getTransaction().getSourceAccount().getAccountNumber();
        }

        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .referenceId(entry.getTransaction().getReferenceId())
                .transactionType(entry.getTransaction().getType().name())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .formattedAmount(entry.getCurrency().format(entry.getAmount()))
                .currency(entry.getCurrency())
                .counterpartyAccountNumber(counterparty)
                .description(description)
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
