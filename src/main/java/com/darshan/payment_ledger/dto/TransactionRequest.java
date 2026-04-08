package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransactionRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Long amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private String description;

    private String idempotencyKey;

    // Required for transfers >= payment.high-value-threshold.
    // Client must call POST /transactions/request-otp first to receive the OTP via SMS.
    private String otp;
}
