package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.Account;
import com.darshan.payment_ledger.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    java.util.List<Account> findByUserId(Long userId);

    long countByStatus(AccountStatus status);
}