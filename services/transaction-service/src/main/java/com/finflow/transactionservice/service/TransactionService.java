package com.finflow.transactionservice.service;

import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.repository.TransactionRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    public Transaction transfer(
            Long senderUserId,
            String token,
            String receiverAccountNumber,
            BigDecimal amount,
            String currency) {

        Long receiverUserId =
                accountClient.getReceiverUserId(token,receiverAccountNumber);

        if (senderUserId.equals(receiverUserId)) {
            throw new RuntimeException("Cannot transfer to your own account");
        }

        Transaction transaction = Transaction.builder()
                .transactionReference(UUID.randomUUID())
                .userId(senderUserId)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);

        accountClient.withdraw(token, amount);
        try {
            accountClient.depositToAccount(
                    token,
                    receiverAccountNumber,
                    amount
            );
        } catch (Exception exception) {
            accountClient.refund(
                    token,
                    senderUserId,
                    amount
            );
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            throw exception;
        }

        return transactionRepository.save(transaction);
    }
}
