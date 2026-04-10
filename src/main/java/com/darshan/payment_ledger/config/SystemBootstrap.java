package com.darshan.payment_ledger.config;

import com.darshan.payment_ledger.entity.Account;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.enums.Role;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.repository.UserRepository;
import com.darshan.payment_ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// On every boot, ensure the singleton SYSTEM_CASH account exists.
// SYSTEM_CASH represents the bank's own cash float (a liability / clearing account).
//   Deposit  = money IN  → DEBIT SYSTEM_CASH, CREDIT customer  → SYSTEM_CASH goes NEGATIVE
//   Withdraw = money OUT → DEBIT customer,   CREDIT SYSTEM_CASH → SYSTEM_CASH goes LESS negative
// Invariant: sum(all ledger balances) == 0 at all times (double-entry).
//
// Also seeds default admin and backoffice users so portals are accessible on first deploy.
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemBootstrap implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    @Override
    public void run(String... args) {

        // ── SYSTEM_CASH account ───────────────────────────────────────────────
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

        // ── Default ADMIN user ────────────────────────────────────────────────
        // Only seeded once. Change password immediately after first login.
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.warn("Seeded default ADMIN user (username=admin). Change password immediately.");
        }

        // ── Default BACKOFFICE user ───────────────────────────────────────────
        if (!userRepository.existsByUsername("backoffice1")) {
            User bo = User.builder()
                    .username("backoffice1")
                    .password(passwordEncoder.encode("Backoffice@1234"))
                    .role(Role.BACKOFFICE)
                    .enabled(true)
                    .build();
            userRepository.save(bo);
            log.warn("Seeded default BACKOFFICE user (username=backoffice1).");
        }
    }
}
