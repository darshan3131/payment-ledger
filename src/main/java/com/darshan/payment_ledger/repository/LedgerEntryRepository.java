package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountId(Long accountId);

    List<LedgerEntry> findByTransactionId(Long transactionId);
}