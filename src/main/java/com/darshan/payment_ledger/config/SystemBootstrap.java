package com.darshan.payment_ledger.config;

import com.darshan.payment_ledger.entity.Account;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// On every boot, ensure the singleton SYSTEM_CASH account exists.
// SYSTEM_CASH represents the bank's own cash float (a liability / clearing account).
//   Deposit  = money IN  → DEBIT SYSTEM_CASH, CREDIT customer  → SYSTEM_CASH goes NEGATIVE
//   Withdraw = money OUT → DEBIT customer,   CREDIT SYSTEM_CASH → SYSTEM_CASH goes LESS negative
// Invariant: sum(all ledger balances) == 0 at all times (double-entry).
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemBootstrap implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.findByAccountNumber(AccountService.SYSTEM_CASH_NUMBER).isEmpty()) {
            Account systemCash = Account.builder()
                    .accountNumber(AccountService.SYSTEM_CASH_NUMBER)
                    .holderName("System Cash Float")
                    .userId(null)
                    .currency(Currency.INR)
                    .balance(0L)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(systemCash);
            log.info("Seeded SYSTEM_CASH account.");
        }
    }
}
