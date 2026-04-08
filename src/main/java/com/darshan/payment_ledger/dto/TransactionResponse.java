package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String referenceId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private Long amount;
    private String formattedAmount;
    private Currency currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
