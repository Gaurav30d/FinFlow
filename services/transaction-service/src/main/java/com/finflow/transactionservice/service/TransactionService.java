package com.finflow.transactionservice.service;

import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.dto.AccountResponse;
import com.finflow.transactionservice.dto.TransactionResult;
import com.finflow.transactionservice.entity.IdempotencyRecord;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.event.TransactionCompletedEvent;
import com.finflow.transactionservice.event.TransactionEventProducer;
import com.finflow.transactionservice.exception.BadRequestException;
import com.finflow.transactionservice.exception.IdempotencyConflictException;
import com.finflow.transactionservice.repository.IdempotencyRecordRepository;
import com.finflow.transactionservice.repository.TransactionRepository;
import com.finflow.transactionservice.util.IdempotencyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AccountClient accountClient;
    private final TransactionEventProducer transactionEventProducer;

    public TransactionResult createTransaction(
            Long userId,
            String token,
            String idempotencyKey,
            TransactionType type,
            BigDecimal amount,
            String currency) {

        if (type == TransactionType.TRANSFER) {
            throw new BadRequestException("Transaction type TRANSFER is not allowed on this endpoint. Use /transactions/transfer instead.");
        }

        if (type != TransactionType.DEPOSIT && type != TransactionType.WITHDRAW) {
            throw new BadRequestException("Only DEPOSIT and WITHDRAW transaction types are supported on this endpoint");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transaction amount must be greater than zero");
        }

        if (currency == null || currency.isBlank()) {
            throw new BadRequestException("Currency is required");
        }

        String payloadHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            payloadHash = IdempotencyUtils.computeHash(
                    "OP=CREATE|TYPE=" + type.name() +
                    "|AMOUNT=" + amount.stripTrailingZeros().toPlainString() +
                    "|CURRENCY=" + currency.trim().toUpperCase()
            );

            Optional<IdempotencyRecord> existingRecordOpt =
                    idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey.trim());

            if (existingRecordOpt.isPresent()) {
                IdempotencyRecord existingRecord = existingRecordOpt.get();
                if (!existingRecord.getRequestHash().equals(payloadHash)) {
                    throw new IdempotencyConflictException(
                            "Idempotency key '" + idempotencyKey + "' was already used with a different request payload."
                    );
                }

                Transaction existingTransaction = transactionRepository
                        .findByTransactionReference(existingRecord.getTransactionReference())
                        .orElseThrow(() -> new IllegalStateException("Transaction reference not found for idempotency record"));

                return new TransactionResult(existingTransaction, true);
            }
        }

        AccountResponse account = accountClient.getMyAccount(token);

        if (type == TransactionType.DEPOSIT) {
            accountClient.deposit(token, amount);
        } else if (type == TransactionType.WITHDRAW) {
            accountClient.withdraw(token, amount);
        }

        UUID txRef = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionReference(txRef)
                .senderUserId(userId)
                .senderAccountNumber(account.getAccountNumber())
                .receiverUserId(userId)
                .receiverAccountNumber(account.getAccountNumber())
                .type(type)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .userId(userId)
                    .idempotencyKey(idempotencyKey.trim())
                    .requestHash(payloadHash)
                    .transactionReference(txRef)
                    .createdAt(Instant.now())
                    .build();
            idempotencyRecordRepository.save(record);
        }

        return new TransactionResult(transaction, false);
    }

    public TransactionResult createTransaction(
            Long userId,
            String token,
            TransactionType type,
            BigDecimal amount,
            String currency) {
        return createTransaction(userId, token, null, type, amount, currency);
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public TransactionResult transfer(
            Long senderUserId,
            String token,
            String idempotencyKey,
            String receiverAccountNumber,
            BigDecimal amount,
            String currency) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transaction amount must be greater than zero");
        }

        if (currency == null || currency.isBlank()) {
            throw new BadRequestException("Currency is required");
        }

        if (receiverAccountNumber == null || receiverAccountNumber.isBlank()) {
            throw new BadRequestException("Receiver account number is required");
        }

        String payloadHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            payloadHash = IdempotencyUtils.computeHash(
                    "OP=TRANSFER|RECEIVER=" + receiverAccountNumber.trim() +
                    "|AMOUNT=" + amount.stripTrailingZeros().toPlainString() +
                    "|CURRENCY=" + currency.trim().toUpperCase()
            );

            Optional<IdempotencyRecord> existingRecordOpt =
                    idempotencyRecordRepository.findByUserIdAndIdempotencyKey(senderUserId, idempotencyKey.trim());

            if (existingRecordOpt.isPresent()) {
                IdempotencyRecord existingRecord = existingRecordOpt.get();
                if (!existingRecord.getRequestHash().equals(payloadHash)) {
                    throw new IdempotencyConflictException(
                            "Idempotency key '" + idempotencyKey + "' was already used with a different request payload."
                    );
                }

                Transaction existingTransaction = transactionRepository
                        .findByTransactionReference(existingRecord.getTransactionReference())
                        .orElseThrow(() -> new IllegalStateException("Transaction reference not found for idempotency record"));

                return new TransactionResult(existingTransaction, true);
            }
        }

        AccountResponse senderAccount = accountClient.getMyAccount(token);

        Long receiverUserId = accountClient.getReceiverUserId(token, receiverAccountNumber);

        if (senderUserId.equals(receiverUserId) || senderAccount.getAccountNumber().equalsIgnoreCase(receiverAccountNumber.trim())) {
            throw new BadRequestException("Cannot transfer to your own account");
        }

        UUID txRef = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionReference(txRef)
                .senderUserId(senderUserId)
                .senderAccountNumber(senderAccount.getAccountNumber())
                .receiverUserId(receiverUserId)
                .receiverAccountNumber(receiverAccountNumber)
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
                    receiverAccountNumber,
                    amount
            );
        } catch (Exception exception) {
            // Compensate sender
            try {
                accountClient.refund(
                        senderUserId,
                        amount
                );

                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);

            } catch (Exception compensationException) {
                /*
                 * Money was debited but compensation failed.
                 * Keep transaction PENDING so it can be recovered later.
                 */
                transactionRepository.save(transaction);
                throw compensationException;
            }

            throw exception;
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        TransactionCompletedEvent event =
                TransactionCompletedEvent.builder()
                        .eventId(UUID.randomUUID())
                        .transactionReference(
                                transaction.getTransactionReference()
                        )
                        .eventType("TRANSACTION_COMPLETED")
                        .senderUserId(transaction.getSenderUserId())
                        .receiverUserId(transaction.getReceiverUserId())
                        .senderAccountNumber(
                                transaction.getSenderAccountNumber()
                        )
                        .receiverAccountNumber(
                                transaction.getReceiverAccountNumber()
                        )
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .transactionType(
                                transaction.getType().name()
                        )
                        .occurredAt(transaction.getCreatedAt())
                        .build();

        transactionEventProducer.publishTransactionCompleted(event);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .userId(senderUserId)
                    .idempotencyKey(idempotencyKey.trim())
                    .requestHash(payloadHash)
                    .transactionReference(txRef)
                    .createdAt(Instant.now())
                    .build();
            idempotencyRecordRepository.save(record);
        }

        return new TransactionResult(transaction, false);
    }

    public TransactionResult transfer(
            Long senderUserId,
            String token,
            String receiverAccountNumber,
            BigDecimal amount,
            String currency) {
        return transfer(senderUserId, token, null, receiverAccountNumber, amount, currency);
    }
}
