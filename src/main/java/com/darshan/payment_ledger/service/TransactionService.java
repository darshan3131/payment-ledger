package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.CashOpRequest;
import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.dto.TransactionRequest;
import com.darshan.payment_ledger.dto.TransactionResponse;
import com.darshan.payment_ledger.entity.*;
import com.darshan.payment_ledger.enums.OutboxStatus;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.enums.TransactionType;
import com.darshan.payment_ledger.exception.AccountNotFoundException;
import com.darshan.payment_ledger.exception.DuplicateTransactionException;
import com.darshan.payment_ledger.exception.InsufficientBalanceException;
import com.darshan.payment_ledger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyService idempotencyKeyService;
    private final AccountService accountService;
    private final OutboxEventRepository outboxEventRepository;
    private final com.darshan.payment_ledger.repository.UserRepository userRepository;
    private final SmsService smsService;
    private final OtpService otpService;

    @org.springframework.beans.factory.annotation.Value("${payment.high-value-threshold:1000000}")
    private long highValueThreshold;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {

        // STEP 1: Idempotency check (Redis)
        if (request.getIdempotencyKey() != null) {
            String existingRefId =
                    idempotencyKeyService.findReferenceId(request.getIdempotencyKey());

            if (existingRefId != null) {
                log.warn("Duplicate request detected for idempotency key: {}",
                        request.getIdempotencyKey());

                Transaction existingTx = transactionRepository
                        .findByReferenceId(existingRefId)
                        .orElseThrow(() -> new DuplicateTransactionException(
                                "Idempotency key exists but transaction not found: "
                                        + request.getIdempotencyKey()));

                return toResponse(existingTx);
            }
        }

        // STEP 2: Validate accounts
        String sysCash = com.darshan.payment_ledger.service.AccountService.SYSTEM_CASH_NUMBER;
        if (sysCash.equals(request.getSourceAccountNumber())
                || sysCash.equals(request.getDestinationAccountNumber())) {
            throw new IllegalArgumentException(
                    "SYSTEM_CASH cannot be used directly. Use /deposit or /withdraw.");
        }
        Account source =
                accountService.findActiveAccount(request.getSourceAccountNumber());
        Account destination =
                accountService.findActiveAccount(request.getDestinationAccountNumber());

        // STEP 2b: High-value OTP check
        if (request.getAmount() >= highValueThreshold) {
            if (request.getOtp() == null || request.getOtp().isBlank()) {
                throw new IllegalArgumentException(
                        "OTP_REQUIRED:" + source.getCurrency().format(highValueThreshold));
            }
            String otpKey = "txn-otp:" + request.getSourceAccountNumber();
            if (!otpService.verifyAndConsume(otpKey, request.getOtp())) {
                throw new IllegalArgumentException("Invalid or expired OTP. Request a new one.");
            }
        }

        // STEP 3: Balance check
        if (source.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: "
                            + source.getCurrency().format(source.getBalance())
                            + ", Required: "
                            + source.getCurrency().format(request.getAmount()));
        }

        // STEP 4: Generate reference ID
        String referenceId = "TXN" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        // STEP 5: Create transaction (PENDING)
        Transaction transaction = Transaction.builder()
                .referenceId(referenceId)
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .type(request.getType())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        transaction = transactionRepository.save(transaction);

        // STEP 6: Debit source
        source.setBalance(source.getBalance() - request.getAmount());
        accountRepository.save(source);

        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(source)
                .entryType("DEBIT")
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .build();

        ledgerEntryRepository.save(debitEntry);

        // STEP 7: Credit destination — apply FX conversion when currencies differ
        long creditAmount = com.darshan.payment_ledger.util.CurrencyConverter.convert(
                request.getAmount(), source.getCurrency(), destination.getCurrency());
        if (source.getCurrency() != destination.getCurrency()) {
            log.info("FX conversion: {} → {} at rate {} (debit={} subunits, credit={} subunits)",
                    source.getCurrency(), destination.getCurrency(),
                    com.darshan.payment_ledger.util.CurrencyConverter.rateDescription(
                            source.getCurrency(), destination.getCurrency()),
                    request.getAmount(), creditAmount);
        }

        destination.setBalance(destination.getBalance() + creditAmount);
        accountRepository.save(destination);

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(destination)
                .entryType("CREDIT")
                .amount(creditAmount)
                .currency(destination.getCurrency())
                .build();

        ledgerEntryRepository.save(creditEntry);

        // STEP 8: Mark COMPLETED
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        // STEP 8b: SMS notifications
        String fmtAmt = source.getCurrency().format(request.getAmount());
        notifyUser(source,      "Sent " + fmtAmt + " to " + destination.getAccountNumber()
                + ". Ref: " + referenceId
                + ". New balance: " + source.getCurrency().format(source.getBalance()));
        notifyUser(destination, "Received " + fmtAmt + " from " + source.getAccountNumber()
                + ". Ref: " + referenceId
                + ". New balance: " + destination.getCurrency().format(destination.getBalance()));

        // STEP 9: Save idempotency key (Redis)
        if (request.getIdempotencyKey() != null) {
            idempotencyKeyService.save(
                    request.getIdempotencyKey(),
                    referenceId
            );
        }

        // STEP 10: Write to Outbox (INSIDE transaction — critical)
        String payload = String.format(
                "{\"referenceId\":\"%s\",\"amount\":%d,\"currency\":\"%s\","
                        + "\"status\":\"COMPLETED\",\"sourceAccount\":\"%s\",\"destinationAccount\":\"%s\"}",
                referenceId,
                request.getAmount(),
                source.getCurrency().name(),
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(referenceId)
                .eventType("TRANSACTION_COMPLETED")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(outboxEvent);

        log.info("Transaction {} COMPLETED: {} from {} to {}",
                referenceId,
                source.getCurrency().format(request.getAmount()),
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber());

        return toResponse(transaction);
    }

    // ─────────────────────────────────────────────────────────────────────
    // High-value OTP: sends OTP to the account owner's phone before a large transfer.
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public void requestTransferOtp(String sourceAccountNumber) {
        Account source = accountRepository.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new com.darshan.payment_ledger.exception.AccountNotFoundException(
                        "Account not found: " + sourceAccountNumber));
        if (source.getUserId() == null)
            throw new IllegalArgumentException("Account has no linked user.");

        // Use Optional.get() after isPresent() instead of ifPresent() so exceptions propagate cleanly.
        var userOpt = userRepository.findById(source.getUserId());
        if (userOpt.isEmpty())
            throw new IllegalArgumentException("Linked user not found for account: " + sourceAccountNumber);

        var user = userOpt.get();
        if (user.getPhone() == null || user.getPhone().isBlank())
            throw new IllegalArgumentException("No phone number on file for this account.");

        // Generate and store the OTP in Redis first — this is the critical step.
        // SMS is best-effort: if Twilio fails, the OTP is still in Redis and
        // the user can check the Spring Boot logs (dev) or use the resend button.
        String otpKey = "txn-otp:" + sourceAccountNumber;
        String otp    = otpService.generateAndStore(otpKey);
        try {
            smsService.send(user.getPhone(),
                    "PayLedger transfer OTP: " + otp + ". Valid 5 mins. Do NOT share. Authorize your transfer.");
        } catch (Exception smsEx) {
            log.warn("SMS notification failed for transfer OTP (OTP still valid in Redis): {}", smsEx.getMessage());
        }
        log.info("Transfer OTP generated for account {} (check console if SMS not received)", sourceAccountNumber);
    }

    public long getHighValueThreshold() { return highValueThreshold; }

    // ─────────────────────────────────────────────────────────────────────
    // Cash operations — deposit/withdraw via the SYSTEM_CASH float account.
    // These are the ONLY ways money enters or exits the ledger universe.
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public TransactionResponse deposit(CashOpRequest request) {
        return cashOp(request, /*isDeposit=*/ true);
    }

    @Transactional
    public TransactionResponse withdraw(CashOpRequest request) {
        return cashOp(request, /*isDeposit=*/ false);
    }

    private TransactionResponse cashOp(CashOpRequest request, boolean isDeposit) {

        // Idempotency replay
        if (request.getIdempotencyKey() != null) {
            String existingRefId = idempotencyKeyService.findReferenceId(request.getIdempotencyKey());
            if (existingRefId != null) {
                Transaction existingTx = transactionRepository.findByReferenceId(existingRefId)
                        .orElseThrow(() -> new DuplicateTransactionException(
                                "Idempotency key exists but transaction not found: " + request.getIdempotencyKey()));
                return toResponse(existingTx);
            }
        }

        Account systemCash = accountRepository.findByAccountNumber(
                com.darshan.payment_ledger.service.AccountService.SYSTEM_CASH_NUMBER)
                .orElseThrow(() -> new AccountNotFoundException("SYSTEM_CASH not seeded. Restart app."));

        Account customer = accountService.findActiveAccount(request.getAccountNumber());

        Account source      = isDeposit ? systemCash : customer;
        Account destination = isDeposit ? customer   : systemCash;

        // Balance check for the SOURCE, with the SYSTEM_CASH bypass (it may go negative).
        if (!source.getAccountNumber().equals(systemCash.getAccountNumber())
                && source.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: "
                            + source.getCurrency().format(source.getBalance())
                            + ", Required: "
                            + source.getCurrency().format(request.getAmount()));
        }

        String referenceId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Transaction transaction = Transaction.builder()
                .referenceId(referenceId)
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.getAmount())
                .currency(customer.getCurrency())
                .type(isDeposit ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        transaction = transactionRepository.save(transaction);

        // Debit source
        source.setBalance(source.getBalance() - request.getAmount());
        accountRepository.save(source);
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transaction(transaction)
                .account(source)
                .entryType("DEBIT")
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .build());

        // Credit destination
        destination.setBalance(destination.getBalance() + request.getAmount());
        accountRepository.save(destination);
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transaction(transaction)
                .account(destination)
                .entryType("CREDIT")
                .amount(request.getAmount())
                .currency(destination.getCurrency())
                .build());

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        if (request.getIdempotencyKey() != null) {
            idempotencyKeyService.save(request.getIdempotencyKey(), referenceId);
        }

        String payload = String.format(
                "{\"referenceId\":\"%s\",\"amount\":%d,\"currency\":\"%s\",\"status\":\"COMPLETED\",\"sourceAccount\":\"%s\",\"destinationAccount\":\"%s\",\"type\":\"%s\"}",
                referenceId, request.getAmount(), customer.getCurrency().name(),
                source.getAccountNumber(), destination.getAccountNumber(),
                transaction.getType().name());
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateId(referenceId)
                .eventType(isDeposit ? "DEPOSIT_COMPLETED" : "WITHDRAWAL_COMPLETED")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build());

        // SMS notification to customer
        String fmtAmt = customer.getCurrency().format(request.getAmount());
        String newBal = customer.getCurrency().format(
                isDeposit ? destination.getBalance() : source.getBalance());
        notifyUser(customer, (isDeposit ? "Deposited " : "Withdrawn ") + fmtAmt
                + (isDeposit ? " into " : " from ") + customer.getAccountNumber()
                + ". New balance: " + newBal + ". Ref: " + referenceId);

        log.info("{} {} {}→{} ref={}",
                isDeposit ? "DEPOSIT" : "WITHDRAWAL",
                customer.getCurrency().format(request.getAmount()),
                source.getAccountNumber(), destination.getAccountNumber(), referenceId);
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String referenceId) {
        Transaction tx = transactionRepository
                .findByReferenceId(referenceId)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found: " + referenceId));

        return toResponse(tx);
    }

    // Paginated: GET /api/v1/transactions?page=0&size=20&sort=createdAt,desc
    //
    // Pageable is populated by Spring from query parameters automatically.
    // JpaRepository.findAll(Pageable) is inherited — no custom query needed.
    // Returns PagedResponse instead of Page<T> to give the frontend a clean JSON shape.
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        Page<Transaction> page = transactionRepository.findAll(pageable);
        return PagedResponse.from(page, this::toResponse);
    }

    // Paginated: GET /api/v1/transactions/account/{accountId}?page=0&size=20
    //
    // Uses the @Query in TransactionRepository that handles OR (sent + received in one query).
    // Previously this fetched ALL transactions for an account and merged them in memory —
    // that breaks at scale. Now the DB does the pagination, the app only gets page-sized slice.
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionsByAccount(Long accountId, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findByAccountId(accountId, pageable);
        return PagedResponse.from(page, this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Transaction reversal — BACKOFFICE/ADMIN only.
    // Creates a mirror REVERSAL transaction (money flows back) and marks original REVERSED.
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public TransactionResponse reverseTransaction(String referenceId, String reason) {
        Transaction original = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + referenceId));

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Only COMPLETED transactions can be reversed. Current status: " + original.getStatus());
        }
        if (original.getType() == TransactionType.DEPOSIT || original.getType() == TransactionType.WITHDRAWAL) {
            throw new IllegalArgumentException("Deposits and withdrawals cannot be reversed here. Use a counter-operation.");
        }

        Account source      = original.getSourceAccount();
        Account destination = original.getDestinationAccount();

        // Check destination still has the funds
        if (destination.getBalance() < original.getAmount()) {
            throw new InsufficientBalanceException(
                    "Destination account has insufficient funds for reversal. Balance: "
                    + destination.getCurrency().format(destination.getBalance()));
        }

        String revRefId = "REV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Transaction reversal = Transaction.builder()
                .referenceId(revRefId)
                .sourceAccount(destination)   // reversed: money goes back
                .destinationAccount(source)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.PENDING)
                .description("Reversal of " + referenceId + (reason != null ? ": " + reason : ""))
                .build();
        reversal = transactionRepository.save(reversal);

        // Debit destination (takes money back)
        destination.setBalance(destination.getBalance() - original.getAmount());
        accountRepository.save(destination);
        ledgerEntryRepository.save(LedgerEntry.builder().transaction(reversal).account(destination)
                .entryType("DEBIT").amount(original.getAmount()).currency(original.getCurrency()).build());

        // Credit source (returns money)
        source.setBalance(source.getBalance() + original.getAmount());
        accountRepository.save(source);
        ledgerEntryRepository.save(LedgerEntry.builder().transaction(reversal).account(source)
                .entryType("CREDIT").amount(original.getAmount()).currency(original.getCurrency()).build());

        reversal.setStatus(TransactionStatus.COMPLETED);
        reversal = transactionRepository.save(reversal);

        // Mark original as REVERSED
        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        // Notify both parties
        String fmtAmt = original.getCurrency().format(original.getAmount());
        notifyUser(source,      "Reversal: " + fmtAmt + " returned to your account. Ref: " + revRefId);
        notifyUser(destination, "Reversal: " + fmtAmt + " deducted from your account. Ref: " + revRefId);

        log.info("Reversed {} → new ref {}", referenceId, revRefId);
        return toResponse(reversal);
    }

    // ── SMS helpers ──────────────────────────────────────────────────────────
    /** Send a non-blocking SMS to the user who owns an account. Silently skips if no phone. */
    private void notifyUser(Account account, String message) {
        if (account.getUserId() == null) return;
        userRepository.findById(account.getUserId()).ifPresent(user -> {
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                smsService.send(user.getPhone(), message);
            }
        });
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceId(tx.getReferenceId())
                .sourceAccountNumber(tx.getSourceAccount().getAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccount().getAccountNumber())
                .amount(tx.getAmount())
                .formattedAmount(tx.getCurrency().format(tx.getAmount()))
                .currency(tx.getCurrency())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}