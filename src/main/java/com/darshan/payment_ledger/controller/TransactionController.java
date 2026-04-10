package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.CashOpRequest;
import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.dto.TransactionRequest;
import com.darshan.payment_ledger.dto.TransactionResponse;
import com.darshan.payment_ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // GET /api/v1/transactions
    // GET /api/v1/transactions?page=0&size=20&sort=createdAt,desc
    //
    // @PageableDefault sets sensible defaults so clients don't need to specify every param.
    // Spring's HandlerMethodArgumentResolver reads ?page, ?size, ?sort query params
    // and builds a Pageable object automatically — no manual parsing needed.
    //
    // sort=createdAt,desc → most recent first (what backoffice dashboards expect)
    // size=20 → 20 rows per page (balance between UX and DB load)
    @GetMapping
    public ResponseEntity<PagedResponse<TransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @PostMapping
    public ResponseEntity<?> processTransaction(@Valid @RequestBody TransactionRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.processTransaction(request));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("OTP_REQUIRED:")) {
                // 428 Precondition Required — client must call /request-otp first
                return ResponseEntity.status(428).body(Map.of(
                        "error", "OTP_REQUIRED",
                        "message", "This transfer requires OTP verification. Call /request-otp first.",
                        "threshold", msg.substring("OTP_REQUIRED:".length())
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // POST /api/v1/transactions/request-otp
    // Sends OTP to the account owner's registered phone.
    // Required before submitting a transfer >= high-value threshold.
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> body) {
        String accountNumber = body.get("sourceAccountNumber");
        if (accountNumber == null || accountNumber.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "sourceAccountNumber required"));
        try {
            transactionService.requestTransferOtp(accountNumber);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to your registered phone.",
                    "threshold", transactionService.getHighValueThreshold()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{referenceId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String referenceId) {
        return ResponseEntity.ok(transactionService.getTransaction(referenceId));
    }

    // GET /api/v1/transactions/account/{accountId}?page=0&size=10
    // Returns all transactions where this account is sender OR receiver, paginated.
    @GetMapping("/account/{accountId}")
    public ResponseEntity<PagedResponse<TransactionResponse>> getTransactionsByAccount(
            @PathVariable Long accountId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId, pageable));
    }

    // POST /api/v1/transactions/{referenceId}/reverse  — BACKOFFICE/ADMIN only
    @PostMapping("/{referenceId}/reverse")
    public ResponseEntity<?> reverseTransaction(
            @PathVariable String referenceId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            return ResponseEntity.ok(transactionService.reverseTransaction(referenceId, reason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/v1/transactions/deposit  — BACKOFFICE/ADMIN only
    // Cash IN: SYSTEM_CASH → customer account. Creates DEBIT+CREDIT ledger entries.
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody CashOpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request));
    }

    // POST /api/v1/transactions/withdraw  — BACKOFFICE/ADMIN only
    // Cash OUT: customer account → SYSTEM_CASH. Creates DEBIT+CREDIT ledger entries.
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody CashOpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request));
    }
}
