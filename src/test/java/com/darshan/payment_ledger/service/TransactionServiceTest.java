package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.TransactionRequest;
import com.darshan.payment_ledger.dto.TransactionResponse;
import com.darshan.payment_ledger.entity.Account;
import com.darshan.payment_ledger.entity.Transaction;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.enums.TransactionType;
import com.darshan.payment_ledger.exception.InsufficientBalanceException;
import com.darshan.payment_ledger.repository.LedgerEntryRepository;
import com.darshan.payment_ledger.repository.OutboxEventRepository;
import com.darshan.payment_ledger.repository.TransactionRepository;
import com.darshan.payment_ledger.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) = use Mockito for creating mocks
// No Spring context loaded — this is a UNIT test, not integration test.
// Pure Java: fast (< 100ms), no DB, no Redis, no Kafka needed.
//
// Mockito: a library that creates fake ("mock") versions of dependencies.
// mock(AccountRepository.class) creates a fake repo that returns whatever you tell it to.
// This lets you test TransactionService in complete isolation.

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    // @Mock creates a fake (Mockito mock) — returns null/empty by default
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private IdempotencyKeyService idempotencyKeyService;
    @Mock private AccountService accountService;
    @Mock private OutboxEventRepository outboxEventRepository;

    // @InjectMocks creates a real TransactionService and injects the @Mock fields
    @InjectMocks private TransactionService transactionService;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        // Build test accounts with known state
        sourceAccount = Account.builder()
                .id(1L).accountNumber("ACC_SOURCE")
                .holderName("Darshan").currency(Currency.INR)
                .balance(100000L).status(AccountStatus.ACTIVE).version(1L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        destinationAccount = Account.builder()
                .id(2L).accountNumber("ACC_DEST")
                .holderName("Raj").currency(Currency.INR)
                .balance(50000L).status(AccountStatus.ACTIVE).version(1L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── TEST 1: Happy Path ─────────────────────────────────────────────────
    @Test
    void processTransaction_success_debitsSourceAndCreditsDestination() {
        // ARRANGE — tell mocks what to return
        TransactionRequest request = new TransactionRequest();
        request.setSourceAccountNumber("ACC_SOURCE");
        request.setDestinationAccountNumber("ACC_DEST");
        request.setAmount(30000L);  // ₹300
        request.setType(TransactionType.TRANSFER);

        when(idempotencyKeyService.findReferenceId(any())).thenReturn(null); // not duplicate
        when(accountService.findActiveAccount("ACC_SOURCE")).thenReturn(sourceAccount);
        when(accountService.findActiveAccount("ACC_DEST")).thenReturn(destinationAccount);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            tx = Transaction.builder()
                    .id(1L).referenceId("TXN_TEST_123")
                    .sourceAccount(sourceAccount).destinationAccount(destinationAccount)
                    .amount(30000L).currency(Currency.INR).type(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            return tx;
        });
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        TransactionResponse response = transactionService.processTransaction(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        // Verify source was debited (balance reduced)
        assertThat(sourceAccount.getBalance()).isEqualTo(70000L); // 100000 - 30000

        // Verify destination was credited (balance increased)
        assertThat(destinationAccount.getBalance()).isEqualTo(80000L); // 50000 + 30000

        // Verify exactly 2 ledger entries were saved (DEBIT + CREDIT)
        verify(ledgerEntryRepository, times(2)).save(any());

        // Verify outbox event was saved (for Kafka publishing)
        verify(outboxEventRepository, times(1)).save(any());
    }

    // ── TEST 2: Insufficient Balance ───────────────────────────────────────
    @Test
    void processTransaction_insufficientBalance_throwsException() {
        TransactionRequest request = new TransactionRequest();
        request.setSourceAccountNumber("ACC_SOURCE");
        request.setDestinationAccountNumber("ACC_DEST");
        request.setAmount(200000L);  // ₹2000 — more than sourceAccount's ₹1000 balance
        request.setType(TransactionType.TRANSFER);

        when(idempotencyKeyService.findReferenceId(any())).thenReturn(null);
        when(accountService.findActiveAccount("ACC_SOURCE")).thenReturn(sourceAccount);
        when(accountService.findActiveAccount("ACC_DEST")).thenReturn(destinationAccount);

        // ASSERT: exception is thrown
        assertThatThrownBy(() -> transactionService.processTransaction(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        // Verify NO ledger entries created, NO transaction saved
        verify(transactionRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    // ── TEST 3: Idempotency — duplicate request returns same result ─────────
    @Test
    void processTransaction_duplicateIdempotencyKey_returnsExistingTransaction() {
        TransactionRequest request = new TransactionRequest();
        request.setSourceAccountNumber("ACC_SOURCE");
        request.setDestinationAccountNumber("ACC_DEST");
        request.setAmount(30000L);
        request.setType(TransactionType.TRANSFER);
        request.setIdempotencyKey("unique-key-123");

        // Simulate: this key was already processed, referenceId already exists
        when(idempotencyKeyService.findReferenceId("unique-key-123")).thenReturn("TXN_EXISTING");

        Transaction existingTx = Transaction.builder()
                .id(1L).referenceId("TXN_EXISTING")
                .sourceAccount(sourceAccount).destinationAccount(destinationAccount)
                .amount(30000L).currency(Currency.INR).type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByReferenceId("TXN_EXISTING"))
                .thenReturn(java.util.Optional.of(existingTx));

        // ACT
        TransactionResponse response = transactionService.processTransaction(request);

        // ASSERT: same transaction returned, no new DB writes
        assertThat(response.getReferenceId()).isEqualTo("TXN_EXISTING");
        verify(accountService, never()).findActiveAccount(any()); // no account lookup needed
        verify(ledgerEntryRepository, never()).save(any());       // no new entries
    }
}
