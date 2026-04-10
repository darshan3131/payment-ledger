package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.AnalyticsResponse;
import com.darshan.payment_ledger.entity.Transaction;
import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import com.darshan.payment_ledger.enums.TransactionStatus;
import com.darshan.payment_ledger.repository.AccountRepository;
import com.darshan.payment_ledger.repository.TransactionRepository;
import com.darshan.payment_ledger.util.CurrencyConverter;
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
        // JOIN FETCH — loads source and destination accounts in a single query,
        // eliminating the 2N SELECT N+1 that LAZY loading would trigger during the stream.
        List<Transaction> allTx = transactionRepository.findAllWithAccounts();
        long totalAccounts  = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus(AccountStatus.ACTIVE);

        // Volume + counts — normalize all amounts to INR for cross-currency aggregation
        long totalVolume = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToLong(t -> CurrencyConverter.convert(t.getAmount(), t.getCurrency(), Currency.INR))
                .sum();

        long completed = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pending   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long failed    = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        long avgSize   = completed > 0 ? totalVolume / completed : 0;

        // Volume by type — normalize each transaction to INR so types can be compared
        Map<String, Long> volumeByType = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        t -> t.getType().name(),
                        Collectors.summingLong(t -> CurrencyConverter.convert(t.getAmount(), t.getCurrency(), Currency.INR))
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
                    // Format using the source account's actual currency
                    Currency srcCurrency = sample.getSourceAccount().getCurrency();
                    return AnalyticsResponse.TopAccount.builder()
                            .accountNumber(e.getKey())
                            .holderName(sample.getSourceAccount().getHolderName())
                            .totalAmount(total)
                            .formattedAmount(srcCurrency.format(total))
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
                    // Format using the destination account's actual currency
                    Currency dstCurrency = sample.getDestinationAccount().getCurrency();
                    return AnalyticsResponse.TopAccount.builder()
                            .accountNumber(e.getKey())
                            .holderName(sample.getDestinationAccount().getHolderName())
                            .totalAmount(total)
                            .formattedAmount(dstCurrency.format(total))
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
