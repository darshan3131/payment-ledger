package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Backoffice creates an Account by picking an existing CUSTOMER user.
// Account number is auto-generated, initial balance is always 0.
// To add opening capital, immediately perform a deposit after creation.
@Data
public class AccountRequest {

    @NotNull(message = "userId (owning customer) is required")
    private Long userId;

    @NotNull(message = "Currency is required")
    private Currency currency;
}
