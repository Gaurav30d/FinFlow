package com.finflow.transactionservice.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSummaryDto {
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private BigDecimal netCashFlow;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalTransfersSent;
    private BigDecimal totalTransfersReceived;
    private long completedTransactionCount;
    private BigDecimal currentBalance;
    private String currency;
    private boolean hasTransactions;
    private List<TransactionSummaryItemDto> recentTransactions;
}
