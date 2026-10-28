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
import com.finflow.transactionservice.exception.InsufficientBalanceException;
import com.finflow.transactionservice.exception.InvalidRecipientException;
import com.finflow.transactionservice.repository.IdempotencyRecordRepository;
import com.finflow.transactionservice.repository.TransactionRepository;
import com.finflow.transactionservice.util.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<IdempotencyRecord> idempotencyCaptor;

    @Captor
    private ArgumentCaptor<TransactionCompletedEvent> eventCaptor;

    private TransactionService transactionService;

    private final Long userId = 101L;
    private final String token = "Bearer sample-jwt-token";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                idempotencyRecordRepository,
                accountClient,
                transactionEventProducer
        );
    }

    private AccountResponse createSampleAccount(Long userId, String accountNumber) {
        AccountResponse response = new AccountResponse();
        response.setId(1L);
        response.setUserId(userId);
        response.setAccountNumber(accountNumber);
        response.setBalance(new BigDecimal("500.00"));
        response.setCurrency("INR");
        response.setStatus("ACTIVE");
        response.setCreatedAt(Instant.now());
        return response;
    }

    // --- 1. createTransaction Tests ---

    @Test
    @DisplayName("createTransaction: DEPOSIT succeeds and records transaction with COMPLETED status")
    void createTransaction_Deposit_Success() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionResult result = transactionService.createTransaction(
                userId,
                token,
                "dep-key-1",
                TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                "INR"
        );

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.transaction().getAmount()).isEqualByComparingTo("100.00");

        verify(accountClient).deposit(token, new BigDecimal("100.00"));
        verify(idempotencyRecordRepository).save(idempotencyCaptor.capture());
        assertThat(idempotencyCaptor.getValue().getIdempotencyKey()).isEqualTo("dep-key-1");
    }

    @Test
    @DisplayName("createTransaction: WITHDRAW succeeds and records transaction with COMPLETED status")
    void createTransaction_Withdraw_Success() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionResult result = transactionService.createTransaction(
                userId,
                token,
                null,
                TransactionType.WITHDRAW,
                new BigDecimal("50.00"),
                "INR"
        );

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getType()).isEqualTo(TransactionType.WITHDRAW);
        verify(accountClient).withdraw(token, new BigDecimal("50.00"));
        verify(idempotencyRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: Rejects TRANSFER type with BadRequestException")
    void createTransaction_RejectTransferType_ThrowsBadRequestException() {
        assertThatThrownBy(() -> transactionService.createTransaction(
                userId,
                token,
                "key-transfer",
                TransactionType.TRANSFER,
                new BigDecimal("50.00"),
                "INR"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Transaction type TRANSFER is not allowed on this endpoint");

        verifyNoInteractions(accountClient);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("createTransaction: Idempotent replay with same key and payload returns existing transaction")
    void createTransaction_IdempotentReplay_ReturnsExistingWithoutReexecution() {
        String key = "dep-key-1";
        String payloadHash = IdempotencyUtils.computeHash("OP=CREATE|TYPE=DEPOSIT|AMOUNT=100|CURRENCY=INR");
        UUID txRef = UUID.randomUUID();

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey(key)
                .requestHash(payloadHash)
                .transactionReference(txRef)
                .createdAt(Instant.now())
                .build();

        Transaction existingTx = Transaction.builder()
                .id(10L)
                .transactionReference(txRef)
                .senderUserId(userId)
                .senderAccountNumber("ACC-101")
                .receiverUserId(userId)
                .receiverAccountNumber("ACC-101")
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.of(existingRecord));
        when(transactionRepository.findByTransactionReference(txRef))
                .thenReturn(Optional.of(existingTx));

        TransactionResult result = transactionService.createTransaction(
                userId,
                token,
                key,
                TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                "INR"
        );

        assertThat(result.replayed()).isTrue();
        assertThat(result.transaction()).isEqualTo(existingTx);
        verifyNoInteractions(accountClient);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: Same key with conflicting payload throws IdempotencyConflictException")
    void createTransaction_ConflictingPayload_ThrowsIdempotencyConflictException() {
        String key = "dep-key-1";
        String originalHash = IdempotencyUtils.computeHash("OP=CREATE|TYPE=DEPOSIT|AMOUNT=100|CURRENCY=INR");

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey(key)
                .requestHash(originalHash)
                .transactionReference(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.of(existingRecord));

        // Different amount: 200.00 instead of 100.00
        assertThatThrownBy(() -> transactionService.createTransaction(
                userId,
                token,
                key,
                TransactionType.DEPOSIT,
                new BigDecimal("200.00"),
                "INR"
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different request payload");

        verifyNoInteractions(accountClient);
        verify(transactionRepository, never()).save(any());
    }

    // --- 2. transfer Tests ---

    @Test
    @DisplayName("transfer: Successful transfer debits sender, credits receiver, saves COMPLETED, and publishes Kafka event")
    void transfer_Success() {
        String receiverAccountNumber = "ACC-202";
        Long receiverUserId = 202L;
        BigDecimal amount = new BigDecimal("75.00");

        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, receiverAccountNumber))
                .thenReturn(receiverUserId);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionResult result = transactionService.transfer(
                userId,
                token,
                "xfer-key-1",
                receiverAccountNumber,
                amount,
                "INR"
        );

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.transaction().getSenderUserId()).isEqualTo(userId);
        assertThat(result.transaction().getReceiverUserId()).isEqualTo(receiverUserId);

        verify(accountClient).withdraw(token, amount);
        verify(accountClient).depositToAccount(receiverAccountNumber, amount);
        verify(transactionEventProducer).publishTransactionCompleted(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAmount()).isEqualByComparingTo(amount);

        verify(idempotencyRecordRepository).save(idempotencyCaptor.capture());
        assertThat(idempotencyCaptor.getValue().getIdempotencyKey()).isEqualTo("xfer-key-1");
    }

    @Test
    @DisplayName("transfer: Idempotent replay with same key and payload returns existing transfer")
    void transfer_IdempotentReplay_ReturnsExistingWithoutReexecution() {
        String key = "xfer-key-1";
        String payloadHash = IdempotencyUtils.computeHash("OP=TRANSFER|RECEIVER=ACC-202|AMOUNT=75|CURRENCY=INR");
        UUID txRef = UUID.randomUUID();

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey(key)
                .requestHash(payloadHash)
                .transactionReference(txRef)
                .createdAt(Instant.now())
                .build();

        Transaction existingTx = Transaction.builder()
                .id(20L)
                .transactionReference(txRef)
                .senderUserId(userId)
                .senderAccountNumber("ACC-101")
                .receiverUserId(202L)
                .receiverAccountNumber("ACC-202")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("75.00"))
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.of(existingRecord));
        when(transactionRepository.findByTransactionReference(txRef))
                .thenReturn(Optional.of(existingTx));

        TransactionResult result = transactionService.transfer(
                userId,
                token,
                key,
                "ACC-202",
                new BigDecimal("75.00"),
                "INR"
        );

        assertThat(result.replayed()).isTrue();
        assertThat(result.transaction()).isEqualTo(existingTx);

        verifyNoInteractions(accountClient);
        verifyNoInteractions(transactionEventProducer);
    }

    @Test
    @DisplayName("transfer: Conflicting payload with same key throws IdempotencyConflictException")
    void transfer_ConflictingPayload_ThrowsIdempotencyConflictException() {
        String key = "xfer-key-1";
        String originalHash = IdempotencyUtils.computeHash("OP=TRANSFER|RECEIVER=ACC-202|AMOUNT=75|CURRENCY=INR");

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey(key)
                .requestHash(originalHash)
                .transactionReference(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.of(existingRecord));

        // Different receiver
        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                key,
                "ACC-999",
                new BigDecimal("75.00"),
                "INR"
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different request payload");

        verifyNoInteractions(accountClient);
        verifyNoInteractions(transactionEventProducer);
    }

    @Test
    @DisplayName("transfer: Rejects self-transfer when receiver userId matches sender")
    void transfer_SelfTransferByUserId_ThrowsBadRequestException() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, "ACC-101"))
                .thenReturn(userId);

        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                null,
                "ACC-101",
                new BigDecimal("50.00"),
                "INR"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transfer to your own account");

        verify(accountClient, never()).withdraw(any(), any());
    }

    @Test
    @DisplayName("transfer: Rejects self-transfer when receiver account matches sender account number")
    void transfer_SelfTransferByAccountNumber_ThrowsBadRequestException() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, "ACC-101"))
                .thenReturn(202L); // Different userId but same account number string

        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                null,
                "ACC-101",
                new BigDecimal("50.00"),
                "INR"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transfer to your own account");

        verify(accountClient, never()).withdraw(any(), any());
    }

    @Test
    @DisplayName("transfer: Invalid recipient throws InvalidRecipientException and halts before debit")
    void transfer_InvalidRecipient_ThrowsInvalidRecipientException() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, "INVALID-ACC"))
                .thenThrow(new InvalidRecipientException("Recipient account not found: INVALID-ACC"));

        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                null,
                "INVALID-ACC",
                new BigDecimal("50.00"),
                "INR"
        ))
                .isInstanceOf(InvalidRecipientException.class)
                .hasMessageContaining("Recipient account not found");

        verify(accountClient, never()).withdraw(any(), any());
        verifyNoInteractions(transactionEventProducer);
    }

    @Test
    @DisplayName("transfer: Insufficient balance during withdraw throws InsufficientBalanceException")
    void transfer_InsufficientBalance_ThrowsInsufficientBalanceException() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, "ACC-202"))
                .thenReturn(202L);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(accountClient).withdraw(token, new BigDecimal("9999.00"));

        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                null,
                "ACC-202",
                new BigDecimal("9999.00"),
                "INR"
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(accountClient, never()).depositToAccount(any(), any());
        verifyNoInteractions(transactionEventProducer);
    }

    @Test
    @DisplayName("transfer: Deposit failure triggers refund and marks transaction FAILED")
    void transfer_DepositFailure_TriggersRefundAndMarksFailed() {
        when(accountClient.getMyAccount(token))
                .thenReturn(createSampleAccount(userId, "ACC-101"));
        when(accountClient.getReceiverUserId(token, "ACC-202"))
                .thenReturn(202L);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(accountClient).withdraw(token, new BigDecimal("50.00"));
        doThrow(new RuntimeException("Downstream network timeout"))
                .when(accountClient).depositToAccount(eq("ACC-202"), any());

        assertThatThrownBy(() -> transactionService.transfer(
                userId,
                token,
                null,
                "ACC-202",
                new BigDecimal("50.00"),
                "INR"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Downstream network timeout");

        // Verify compensation refund called
        verify(accountClient).refund(userId, new BigDecimal("50.00"));
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        verifyNoInteractions(transactionEventProducer);
    }
}
