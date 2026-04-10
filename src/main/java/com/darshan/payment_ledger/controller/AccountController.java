package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.AccountRequest;
import com.darshan.payment_ledger.dto.AccountResponse;
import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Role;
import com.darshan.payment_ledger.repository.UserRepository;
import com.darshan.payment_ledger.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final com.darshan.payment_ledger.repository.AccountRepository accountRepository;

    // GET /api/v1/accounts
    // GET /api/v1/accounts?page=1&size=10&sort=holderName,asc
    @GetMapping
    public ResponseEntity<PagedResponse<AccountResponse>> getAllAccounts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(accountService.getAllAccounts(pageable));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    // GET /api/v1/accounts/available-customers
    // GET /api/v1/accounts/available-customers?unlinked=true  — only customers with NO accounts yet
    // GET /api/v1/accounts/available-customers?unlinked=false — all CUSTOMER users (default)
    @GetMapping("/available-customers")
    public ResponseEntity<List<Map<String, Object>>> getAvailableCustomers(
            @RequestParam(defaultValue = "false") boolean unlinked) {

        List<Map<String, Object>> customers = userRepository.findByRole(Role.CUSTOMER)
                .stream()
                .filter(u -> !unlinked || accountRepository.findByUserId(u.getId()).isEmpty())
                .map(u -> {
                    long accountCount = accountRepository.findByUserId(u.getId()).size();
                    return Map.<String, Object>of(
                            "id",           u.getId(),
                            "username",     u.getUsername(),
                            "accountCount", accountCount
                    );
                })
                .toList();
        return ResponseEntity.ok(customers);
    }

    // GET /api/v1/accounts/my  — customer: all accounts linked to the logged-in user
    @GetMapping("/my")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            org.springframework.security.core.Authentication auth) {
        String username = auth.getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<AccountResponse> accounts = accountService.getAccountsByUserId(user.getId());
        return ResponseEntity.ok(accounts);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam AccountStatus status) {
        return ResponseEntity.ok(accountService.updateStatus(id, status));
    }
}
