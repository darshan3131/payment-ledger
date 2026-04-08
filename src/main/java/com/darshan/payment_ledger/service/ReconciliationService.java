package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.entity.Transaction;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ReconciliationService: the safety net for the PENDING-first pattern.
//
// Why does PENDING-first need a safety net?
// In TransactionService.processTransaction(), we save the transaction as PENDING
// before touching any balances. This is intentional — if the app crashes mid-transfer,
// we have a record of the attempt.
//
// But without this service, those stuck PENDING rows would sit forever.
// Reconciliation detects them and moves them to FAILED, keeping the ledger clean.
//
// Interview talking point: "How do you handle partial failures in distributed transactions?"
// Answer: "We use a PENDING-first pattern with a scheduled reconciliation job.
// If a transaction doesn't reach COMPLETED within 5 minutes, it's auto-failed
// and the client can use the idempotency key to safely retry."

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final TransactionRepository transactionRepository;

    // Runs every 60 seconds (fixedDelay = ms after last completion).
    // fixedDelay vs fixedRate:
    //   fixedRate: starts every N ms regardless of how long the job took (can pile up)
    //   fixedDelay: waits N ms AFTER the job finishes before starting again (safer)
    // For financial reconciliation, fixedDelay is correct — we never want two runs overlapping.
    // initialDelay = 30s: wait until the app is fully started before first run.
    // Without this, the job fires immediately at startup — before Flyway has
    // created the tables — causing a "table doesn't exist" error.
    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    @Transactional
    public void reconcileStuckTransactions() {

        // Any transaction still PENDING after 5 minutes is considered stuck.
        // Normal transactions complete in milliseconds — 5 minutes is very conservative.
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

        List<Transaction> stuckTransactions = transactionRepository
                .findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoff);

        if (stuckTransactions.isEmpty()) {
            log.debug("Reconciliation: no stuck transactions found.");
            return;
        }

        log.warn("Reconciliation: found {} stuck PENDING transaction(s). Marking as FAILED.",
                stuckTransactions.size());

        for (Transaction tx : stuckTransactions) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            log.warn("Reconciliation: transaction {} marked FAILED. " +
                     "Created at: {}, Amount: {}, Source: {}, Destination: {}",
                    tx.getReferenceId(),
                    tx.getCreatedAt(),
                    tx.getAmount(),
                    tx.getSourceAccount().getAccountNumber(),
                    tx.getDestinationAccount().getAccountNumber());
        }

        log.info("Reconciliation complete. {} transaction(s) resolved.", stuckTransactions.size());
    }
}
