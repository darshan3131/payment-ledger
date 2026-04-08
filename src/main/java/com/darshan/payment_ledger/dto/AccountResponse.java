package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long userId;
    private String holderName;
    private Currency currency;
    private Long balance;
    private String formattedBalance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
