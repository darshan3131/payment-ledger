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
        // Create if missing OR repair if password is not a valid BCrypt hash.
        // BCrypt hashes always start with $2a$ or $2b$. Anything else means
        // the user was created with plain-text (e.g. a manual DB insert) and
        // Spring Security will refuse to authenticate it.
        seedOrRepair("admin",      "Admin@1234",      Role.ADMIN);
        seedOrRepair("backoffice1","Backoffice@1234",  Role.BACKOFFICE);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    // Creates the user if absent, OR re-hashes the password if the stored value
    // is not a valid BCrypt hash (happens when a user was inserted manually with
    // plain-text).  BCrypt hashes always start with $2a$ or $2b$.
    private void seedOrRepair(String username, String rawPassword, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
            existing -> {
                String stored = existing.getPassword();
                boolean isValidBcrypt = stored != null
                        && (stored.startsWith("$2a$") || stored.startsWith("$2b$"));
                // If the stored hash is valid BCrypt AND matches the expected password → nothing to do.
                // If it's non-BCrypt OR the hash doesn't match the expected seed password,
                // re-encode and save.  This self-heals any password mismatch on every boot.
                if (isValidBcrypt && passwordEncoder.matches(rawPassword, stored) && existing.isEnabled()) {
                    return;
                }
                existing.setPassword(passwordEncoder.encode(rawPassword));
                existing.setEnabled(true);  // always restore — a disabled seed user locks everyone out
                userRepository.save(existing);
                log.warn("Reset password/enabled flag for user '{}' (was non-BCrypt, hash mismatch, or disabled).", username);
            },
            () -> {
                // User doesn't exist at all — create from scratch.
                User u = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .build();
                userRepository.save(u);
                log.info("Seeded default user '{}'.", username);
            }
        );
    }
}
