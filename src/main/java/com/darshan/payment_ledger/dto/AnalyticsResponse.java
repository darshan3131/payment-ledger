package com.darshan.payment_ledger.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private long totalAccounts;
    private long activeAccounts;
    private long totalTransactions;
    private long completedTransactions;
    private long pendingTransactions;
    private long failedTransactions;
    private long totalVolumeProcessed;       // in paise
    private String formattedTotalVolume;
    private long averageTransactionSize;     // in paise
    private String formattedAverageSize;
    private Map<String, Long> volumeByType;         // TRANSFER: 500000, PAYMENT: 200000
    private Map<String, Long> countByStatus;        // COMPLETED: 45, FAILED: 2
    private List<TopAccount> topSenders;
    private List<TopAccount> topReceivers;

    @Data @Builder
    public static class TopAccount {
        private String accountNumber;
        private String holderName;
        private long totalAmount;
        private String formattedAmount;
        private long count;
    }
}
