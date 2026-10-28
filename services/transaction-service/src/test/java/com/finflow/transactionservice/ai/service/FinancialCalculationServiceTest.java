package com.finflow.transactionservice.ai.service;

import com.finflow.transactionservice.ai.dto.FinancialSummaryDto;
import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.dto.AccountResponse;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialCalculationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    private FinancialCalculationService calculationService;

    private final Long userId = 101L;
    private final String userToken = "Bearer dummy.jwt.token";

    @BeforeEach
    void setUp() {
        calculationService = new FinancialCalculationService(transactionRepository, accountClient);
    }

    private Transaction createTransaction(
            UUID ref,
            Long senderId,
            String senderAcc,
            Long receiverId,
            String receiverAcc,
            TransactionType type,
            BigDecimal amount,
            TransactionStatus status) {

        return Transaction.builder()
                .transactionReference(ref)
                .senderUserId(senderId)
                .senderAccountNumber(senderAcc)
                .receiverUserId(receiverId)
                .receiverAccountNumber(receiverAcc)
                .type(type)
                .amount(amount)
                .currency("INR")
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Accurately calculates total inflow, outflow, deposits, withdrawals, and transfers for user")
    void calculateSummary_CalculatesAccurateTotals() {
        Transaction deposit = createTransaction(
                UUID.randomUUID(), userId, "ACC101", userId, "ACC101",
                TransactionType.DEPOSIT, new BigDecimal("5000.00"), TransactionStatus.COMPLETED);

        Transaction withdrawal = createTransaction(
                UUID.randomUUID(), userId, "ACC101", userId, "ACC101",
                TransactionType.WITHDRAW, new BigDecimal("1000.00"), TransactionStatus.COMPLETED);

        Transaction transferSent = createTransaction(
                UUID.randomUUID(), userId, "ACC101", 202L, "ACC202",
                TransactionType.TRANSFER, new BigDecimal("1500.00"), TransactionStatus.COMPLETED);

        Transaction transferReceived = createTransaction(
                UUID.randomUUID(), 303L, "ACC303", userId, "ACC101",
                TransactionType.TRANSFER, new BigDecimal("2000.00"), TransactionStatus.COMPLETED);

        when(transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.COMPLETED))
                .thenReturn(List.of(deposit, withdrawal, transferSent, transferReceived));

        AccountResponse account = new AccountResponse();
        account.setBalance(new BigDecimal("4500.00"));
        account.setCurrency("INR");
        when(accountClient.getMyAccount(userToken)).thenReturn(account);

        FinancialSummaryDto summary = calculationService.calculateSummary(userId, userToken);

        assertThat(summary.isHasTransactions()).isTrue();
        assertThat(summary.getCompletedTransactionCount()).isEqualTo(4);
        // Inflow = Deposit (5000) + Transfer Received (2000) = 7000.00
        assertThat(summary.getTotalInflow()).isEqualByComparingTo("7000.00");
        // Outflow = Withdrawal (1000) + Transfer Sent (1500) = 2500.00
        assertThat(summary.getTotalOutflow()).isEqualByComparingTo("2500.00");
        // Net Cash Flow = 7000 - 2500 = 4500.00
        assertThat(summary.getNetCashFlow()).isEqualByComparingTo("4500.00");
        assertThat(summary.getTotalDeposits()).isEqualByComparingTo("5000.00");
        assertThat(summary.getTotalWithdrawals()).isEqualByComparingTo("1000.00");
        assertThat(summary.getTotalTransfersSent()).isEqualByComparingTo("1500.00");
        assertThat(summary.getTotalTransfersReceived()).isEqualByComparingTo("2000.00");
        assertThat(summary.getCurrentBalance()).isEqualByComparingTo("4500.00");
        assertThat(summary.getRecentTransactions()).hasSize(4);
    }

    @Test
    @DisplayName("Empty transaction history returns zero metrics and hasTransactions false")
    void calculateSummary_EmptyTransactionHistory() {
        when(transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.COMPLETED))
                .thenReturn(Collections.emptyList());

        AccountResponse account = new AccountResponse();
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency("INR");
        when(accountClient.getMyAccount(userToken)).thenReturn(account);

        FinancialSummaryDto summary = calculationService.calculateSummary(userId, userToken);

        assertThat(summary.isHasTransactions()).isFalse();
        assertThat(summary.getCompletedTransactionCount()).isEqualTo(0);
        assertThat(summary.getTotalInflow()).isEqualByComparingTo("0.00");
        assertThat(summary.getTotalOutflow()).isEqualByComparingTo("0.00");
        assertThat(summary.getNetCashFlow()).isEqualByComparingTo("0.00");
        assertThat(summary.getRecentTransactions()).isEmpty();
    }

    @Test
    @DisplayName("User data isolation: queries database strictly with authenticated userId")
    void calculateSummary_UserDataIsolation() {
        when(transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.COMPLETED))
                .thenReturn(Collections.emptyList());

        calculationService.calculateSummary(userId, null);

        verify(transactionRepository).findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(TransactionStatus.COMPLETED));
    }

    @Test
    @DisplayName("Self transfers do not distort inflow and outflow figures")
    void calculateSummary_SelfTransfer_HandledCleanly() {
        Transaction selfTransfer = createTransaction(
                UUID.randomUUID(), userId, "ACC101", userId, "ACC101",
                TransactionType.TRANSFER, new BigDecimal("800.00"), TransactionStatus.COMPLETED);

        when(transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.COMPLETED))
                .thenReturn(List.of(selfTransfer));

        FinancialSummaryDto summary = calculationService.calculateSummary(userId, null);

        assertThat(summary.isHasTransactions()).isTrue();
        assertThat(summary.getTotalInflow()).isEqualByComparingTo("0.00");
        assertThat(summary.getTotalOutflow()).isEqualByComparingTo("0.00");
        assertThat(summary.getNetCashFlow()).isEqualByComparingTo("0.00");
    }
}
