package com.darshan.payment_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// Body for /api/v1/transactions/deposit and /withdraw.
// Backoffice staff stamps cash in or out of a customer account.
@Data
public class CashOpRequest {

    @NotBlank(message = "accountNumber is required")
    private String accountNumber;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Long amount;

    private String description;

    private String idempotencyKey;
}
