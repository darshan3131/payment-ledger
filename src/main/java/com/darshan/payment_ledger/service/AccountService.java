package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.AccountRequest;
import com.darshan.payment_ledger.dto.AccountResponse;
import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.entity.Account;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Role;
import com.darshan.payment_ledger.exception.AccountNotActiveException;
import com.darshan.payment_ledger.exception.AccountNotFoundException;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.repository.UserRepository;
import com.darshan.payment_ledger.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    public static final String SYSTEM_CASH_NUMBER = "SYSTEM_CASH";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        // Look up the owning user.
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + request.getUserId()));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Payment accounts can only be created for CUSTOMER users. "
                    + user.getUsername() + " has role " + user.getRole());
        }

        // Multiple accounts per user are allowed — no single-account restriction.
        // The first account created is still written to user.accountNumber for legacy login lookups.

        String accountNumber = generateAccountNumber();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateAccountNumber();
        }

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .holderName(user.getUsername())
                .userId(user.getId())
                .currency(request.getCurrency())
                .balance(0L)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);

        // Write the denormalized account number back to user only if they don't have one yet.
        // (Keeps the first account as the "primary" for login lookup without overwriting it.)
        if (user.getAccountNumber() == null || user.getAccountNumber().isBlank()) {
            user.setAccountNumber(saved.getAccountNumber());
            userRepository.save(user);
        }

        log.info("Account {} linked to user {} (id={})",
                saved.getAccountNumber(), user.getUsername(), user.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return toResponse(account);
    }

    // Paginated list — hides SYSTEM_CASH from normal users.
    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAllAccounts(Pageable pageable) {
        Page<Account> page = accountRepository.findAll(pageable);
        return PagedResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccountsList() {
        return accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateStatus(Long id, AccountStatus newStatus) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        if (SYSTEM_CASH_NUMBER.equals(account.getAccountNumber())) {
            throw new IllegalArgumentException("SYSTEM_CASH status cannot be changed.");
        }
        account.setStatus(newStatus);
        log.info("Account {} status updated to {}", account.getAccountNumber(), newStatus);
        Account saved = accountRepository.save(account);

        // Notify account owner on FROZEN or CLOSED.
        // Wrapped in try-catch: SMS failure must NEVER roll back the status update.
        // The DB write is the critical operation; notification is best-effort.
        try {
            if ((newStatus == AccountStatus.FROZEN || newStatus == AccountStatus.CLOSED)
                    && account.getUserId() != null) {
                userRepository.findById(account.getUserId()).ifPresent(user -> {
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        String msg = newStatus == AccountStatus.FROZEN
                                ? "Your PayLedger account " + account.getAccountNumber() + " has been FROZEN. Contact support to resolve."
                                : "Your PayLedger account " + account.getAccountNumber() + " has been CLOSED. Contact support for details.";
                        smsService.send(user.getPhone(), msg);
                    }
                });
            }
        } catch (Exception notifyEx) {
            log.warn("SMS notification failed for account {} status change (non-fatal): {}",
                    account.getAccountNumber(), notifyEx.getMessage());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Package-accessible — used by TransactionService
    Account findActiveAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account " + accountNumber + " is not active. Current status: " + account.getStatus());
        }
        return account;
    }

    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .holderName(account.getHolderName())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .formattedBalance(account.getCurrency().format(account.getBalance()))
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
