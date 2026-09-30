package com.finflow.transactionservice.service;

import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Transactional
    public Transaction createTransaction(
            Long userId,
            String token,
            TransactionType type,
            BigDecimal amount,
            String currency
            ) {

        if (type == TransactionType.DEPOSIT) {
            accountClient.deposit(token, amount);
        }else if(type==TransactionType.WITHDRAW) {
            accountClient.withdraw(token, amount);
        }

        Transaction transaction = Transaction.builder()
                .transactionReference(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {

        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }
}
