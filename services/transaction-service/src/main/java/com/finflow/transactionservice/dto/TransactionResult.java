package com.finflow.transactionservice.dto;

import com.finflow.transactionservice.entity.Transaction;

public record TransactionResult(Transaction transaction, boolean replayed) {
}
