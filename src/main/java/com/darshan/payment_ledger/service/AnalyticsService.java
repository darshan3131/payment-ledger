package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.AnalyticsResponse;
import com.darshan.payment_ledger.entity.Transaction;
import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        List<Transaction> allTx = transactionRepository.findAll();
        long totalAccounts  = accountRepository.count();
        long activeAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getStatus().name().equals("ACTIVE")).count();

        // Volume + counts
        long totalVolume = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToLong(Transaction::getAmount).sum();

        long completed = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pending   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long failed    = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        long avgSize   = completed > 0 ? totalVolume / completed : 0;

        // Volume by type
        Map<String, Long> volumeByType = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        t -> t.getType().name(),
                        Collectors.summingLong(Transaction::getAmount)
                ));

        // Count by status
        Map<String, Long> countByStatus = allTx.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()
                ));

        // Top 5 senders (by total amount sent)
        List<AnalyticsResponse.TopAccount> topSenders = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        t -> t.getSourceAccount().getAccountNumber(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(e -> {
                    Transaction sample = e.getValue().get(0);
                    long total = e.getValue().stream().mapToLong(Transaction::getAmount).sum();
                    return AnalyticsResponse.TopAccount.builder()
                            .accountNumber(e.getKey())
                            .holderName(sample.getSourceAccount().getHolderName())
                            .totalAmount(total)
                            .formattedAmount(Currency.INR.format(total))
                            .count(e.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparingLong(AnalyticsResponse.TopAccount::getTotalAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Top 5 receivers (by total amount received)
        List<AnalyticsResponse.TopAccount> topReceivers = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        t -> t.getDestinationAccount().getAccountNumber(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(e -> {
                    Transaction sample = e.getValue().get(0);
                    long total = e.getValue().stream().mapToLong(Transaction::getAmount).sum();
                    return AnalyticsResponse.TopAccount.builder()
                            .accountNumber(e.getKey())
                            .holderName(sample.getDestinationAccount().getHolderName())
                            .totalAmount(total)
                            .formattedAmount(Currency.INR.format(total))
                            .count(e.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparingLong(AnalyticsResponse.TopAccount::getTotalAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .totalTransactions(allTx.size())
                .completedTransactions(completed)
                .pendingTransactions(pending)
                .failedTransactions(failed)
                .totalVolumeProcessed(totalVolume)
                .formattedTotalVolume(Currency.INR.format(totalVolume))
                .averageTransactionSize(avgSize)
                .formattedAverageSize(Currency.INR.format(avgSize))
                .volumeByType(volumeByType)
                .countByStatus(countByStatus)
                .topSenders(topSenders)
                .topReceivers(topReceivers)
                .build();
    }
}
