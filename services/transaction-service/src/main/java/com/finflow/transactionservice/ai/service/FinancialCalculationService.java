package com.finflow.transactionservice.ai.service;

import com.finflow.transactionservice.ai.dto.FinancialSummaryDto;
import com.finflow.transactionservice.ai.dto.TransactionSummaryItemDto;
import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.dto.AccountResponse;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialCalculationService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Transactional(readOnly = true)
    public FinancialSummaryDto calculateSummary(Long userId, String token) {
        List<Transaction> completedTransactions = transactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.COMPLETED);

        BigDecimal currentBalance = null;
        String currency = "INR";

        if (token != null && !token.isBlank()) {
            try {
                AccountResponse account = accountClient.getMyAccount(token);
                if (account != null) {
                    currentBalance = account.getBalance();
                    if (account.getCurrency() != null && !account.getCurrency().isBlank()) {
                        currency = account.getCurrency();
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve live account balance for user {}: {}", userId, ex.getMessage());
            }
        }

        if (completedTransactions.isEmpty()) {
            return FinancialSummaryDto.builder()
                    .totalInflow(BigDecimal.ZERO.setScale(2))
                    .totalOutflow(BigDecimal.ZERO.setScale(2))
                    .netCashFlow(BigDecimal.ZERO.setScale(2))
                    .totalDeposits(BigDecimal.ZERO.setScale(2))
                    .totalWithdrawals(BigDecimal.ZERO.setScale(2))
                    .totalTransfersSent(BigDecimal.ZERO.setScale(2))
                    .totalTransfersReceived(BigDecimal.ZERO.setScale(2))
                    .completedTransactionCount(0)
                    .currentBalance(currentBalance != null ? currentBalance : BigDecimal.ZERO.setScale(2))
                    .currency(currency)
                    .hasTransactions(false)
                    .recentTransactions(Collections.emptyList())
                    .build();
        }

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersSent = BigDecimal.ZERO;
        BigDecimal totalTransfersReceived = BigDecimal.ZERO;

        List<TransactionSummaryItemDto> items = new ArrayList<>();

        for (Transaction t : completedTransactions) {
            BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            if (currency.equals("INR") && t.getCurrency() != null && !t.getCurrency().isBlank()) {
                currency = t.getCurrency();
            }

            boolean isSender = userId.equals(t.getSenderUserId());
            boolean isReceiver = userId.equals(t.getReceiverUserId());
            TransactionType type = t.getType();

            if (type == TransactionType.DEPOSIT && isReceiver) {
                totalDeposits = totalDeposits.add(amount);
                items.add(TransactionSummaryItemDto.builder()
                        .transactionReference(t.getTransactionReference())
                        .type("DEPOSIT")
                        .amount(amount)
                        .currency(t.getCurrency())
                        .description("Deposit into your account")
                        .createdAt(t.getCreatedAt())
                        .build());

            } else if (type == TransactionType.WITHDRAW && isSender) {
                totalWithdrawals = totalWithdrawals.add(amount);
                items.add(TransactionSummaryItemDto.builder()
                        .transactionReference(t.getTransactionReference())
                        .type("WITHDRAW")
                        .amount(amount)
                        .currency(t.getCurrency())
                        .description("Withdrawal from your account")
                        .createdAt(t.getCreatedAt())
                        .build());

            } else if (type == TransactionType.TRANSFER) {
                if (isSender && isReceiver) {
                    items.add(TransactionSummaryItemDto.builder()
                            .transactionReference(t.getTransactionReference())
                            .type("TRANSFER")
                            .amount(amount)
                            .currency(t.getCurrency())
                            .description("Self transfer")
                            .createdAt(t.getCreatedAt())
                            .build());
                } else if (isSender) {
                    totalTransfersSent = totalTransfersSent.add(amount);
                    items.add(TransactionSummaryItemDto.builder()
                            .transactionReference(t.getTransactionReference())
                            .type("TRANSFER_SENT")
                            .amount(amount)
                            .currency(t.getCurrency())
                            .description("Transfer sent to account " + maskAccountNumber(t.getReceiverAccountNumber()))
                            .createdAt(t.getCreatedAt())
                            .build());
                } else if (isReceiver) {
                    totalTransfersReceived = totalTransfersReceived.add(amount);
                    items.add(TransactionSummaryItemDto.builder()
                            .transactionReference(t.getTransactionReference())
                            .type("TRANSFER_RECEIVED")
                            .amount(amount)
                            .currency(t.getCurrency())
                            .description("Transfer received from account " + maskAccountNumber(t.getSenderAccountNumber()))
                            .createdAt(t.getCreatedAt())
                            .build());
                }
            }
        }

        BigDecimal totalInflow = totalDeposits.add(totalTransfersReceived);
        BigDecimal totalOutflow = totalWithdrawals.add(totalTransfersSent);
        BigDecimal netCashFlow = totalInflow.subtract(totalOutflow);

        if (currentBalance == null) {
            currentBalance = netCashFlow;
        }

        List<TransactionSummaryItemDto> recent = items.size() > 10 ? items.subList(0, 10) : items;

        return FinancialSummaryDto.builder()
                .totalInflow(totalInflow.setScale(2))
                .totalOutflow(totalOutflow.setScale(2))
                .netCashFlow(netCashFlow.setScale(2))
                .totalDeposits(totalDeposits.setScale(2))
                .totalWithdrawals(totalWithdrawals.setScale(2))
                .totalTransfersSent(totalTransfersSent.setScale(2))
                .totalTransfersReceived(totalTransfersReceived.setScale(2))
                .completedTransactionCount(completedTransactions.size())
                .currentBalance(currentBalance.setScale(2))
                .currency(currency)
                .hasTransactions(true)
                .recentTransactions(recent)
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber != null ? accountNumber : "";
        }
        return "••••" + accountNumber.substring(accountNumber.length() - 4);
    }
}
