package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.Transaction;
import com.darshan.payment_ledger.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    // Used by AnalyticsService — JOIN FETCH eliminates the N+1 that occurs when the analytics
    // stream accesses sourceAccount/destinationAccount on LAZY-loaded associations.
    // Without this, loading N transactions fires 2N additional SELECT queries (one per account per side).
    @Query("SELECT t FROM Transaction t JOIN FETCH t.sourceAccount JOIN FETCH t.destinationAccount")
    List<Transaction> findAllWithAccounts();

    List<Transaction> findBySourceAccountId(Long accountId);

    List<Transaction> findByDestinationAccountId(Long accountId);

    List<Transaction> findByStatus(TransactionStatus status);

    // Used by ReconciliationService to find stuck transactions.
    // "Give me all transactions that are still PENDING and were created before X time."
    // X = now minus 5 minutes. These are transactions that never reached COMPLETED or FAILED —
    // likely because the app crashed or threw an unhandled exception mid-flight.
    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime cutoff);

    // Paginated: all transactions for a specific account (as sender OR receiver).
    // WHY @Query? Spring Data can't derive a single OR query across two foreign keys
    // from the method name alone — it would need two separate method calls + merging.
    // @Query lets us express "source OR destination" in one DB round-trip.
    // countQuery is REQUIRED when using JOIN in the main query — Spring needs a separate
    // count(*) query to compute totalElements without fetching all rows.
    @Query(value = "SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId",
           countQuery = "SELECT COUNT(t) FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}