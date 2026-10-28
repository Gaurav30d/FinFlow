package com.finflow.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.transactionservice.dto.CreateTransactionRequest;
import com.finflow.transactionservice.dto.TransactionResponse;
import com.finflow.transactionservice.dto.TransactionResult;
import com.finflow.transactionservice.dto.TransferRequest;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import com.finflow.transactionservice.exception.BadRequestException;
import com.finflow.transactionservice.exception.GlobalExceptionHandler;
import com.finflow.transactionservice.exception.IdempotencyConflictException;
import com.finflow.transactionservice.exception.InsufficientBalanceException;
import com.finflow.transactionservice.exception.InvalidRecipientException;
import com.finflow.transactionservice.mapper.TransactionMapper;
import com.finflow.transactionservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionController transactionController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long userId = 101L;
    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Transaction createSampleTransaction(TransactionType type, BigDecimal amount) {
        return Transaction.builder()
                .id(1L)
                .transactionReference(UUID.randomUUID())
                .senderUserId(userId)
                .senderAccountNumber("ACC-101")
                .receiverUserId(202L)
                .receiverAccountNumber("ACC-202")
                .type(type)
                .amount(amount)
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    private TransactionResponse createSampleResponse(TransactionType type, BigDecimal amount) {
        return TransactionResponse.builder()
                .transactionReference(UUID.randomUUID())
                .type(type)
                .direction("OUTGOING")
                .amount(amount)
                .currency("INR")
                .counterpartyAccount("••••0202")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /transactions with TRANSFER type returns 400 Bad Request")
    void createTransaction_TransferType_Returns400() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.TRANSFER);
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("INR");

        when(transactionService.createTransaction(eq(userId), any(), any(), eq(TransactionType.TRANSFER), any(), any()))
                .thenThrow(new BadRequestException("Transaction type TRANSFER is not allowed on this endpoint"));

        mockMvc.perform(post("/transactions")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transaction type TRANSFER is not allowed on this endpoint"));
    }

    @Test
    @DisplayName("POST /transactions with DEPOSIT returns 201 Created on fresh execution")
    void createTransaction_Deposit_Returns201() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.DEPOSIT);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        Transaction tx = createSampleTransaction(TransactionType.DEPOSIT, new BigDecimal("100.00"));
        when(transactionService.createTransaction(eq(userId), any(), eq("key-1"), eq(TransactionType.DEPOSIT), eq(new BigDecimal("100.00")), eq("INR")))
                .thenReturn(new TransactionResult(tx, false));
        when(transactionMapper.toResponse(tx, userId))
                .thenReturn(createSampleResponse(TransactionType.DEPOSIT, new BigDecimal("100.00")));

        mockMvc.perform(post("/transactions")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("POST /transactions idempotent replay returns 200 OK")
    void createTransaction_Replay_Returns200() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.DEPOSIT);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        Transaction tx = createSampleTransaction(TransactionType.DEPOSIT, new BigDecimal("100.00"));
        when(transactionService.createTransaction(eq(userId), any(), eq("key-1"), eq(TransactionType.DEPOSIT), eq(new BigDecimal("100.00")), eq("INR")))
                .thenReturn(new TransactionResult(tx, true));
        when(transactionMapper.toResponse(tx, userId))
                .thenReturn(createSampleResponse(TransactionType.DEPOSIT, new BigDecimal("100.00")));

        mockMvc.perform(post("/transactions")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("POST /transactions with conflicting idempotency payload returns 409 Conflict")
    void createTransaction_ConflictingPayload_Returns409() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.DEPOSIT);
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("INR");

        when(transactionService.createTransaction(eq(userId), any(), eq("key-1"), any(), any(), any()))
                .thenThrow(new IdempotencyConflictException("Idempotency key 'key-1' was already used with a different request payload"));

        mockMvc.perform(post("/transactions")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Idempotency key 'key-1' was already used with a different request payload"));
    }

    @Test
    @DisplayName("POST /transactions/transfer self-transfer returns 400 Bad Request")
    void transfer_SelfTransfer_Returns400() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("ACC-101");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("INR");

        when(transactionService.transfer(eq(userId), any(), any(), eq("ACC-101"), any(), any()))
                .thenThrow(new BadRequestException("Cannot transfer to your own account"));

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot transfer to your own account"));
    }

    @Test
    @DisplayName("POST /transactions/transfer invalid recipient returns 400 Bad Request")
    void transfer_InvalidRecipient_Returns400() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("UNKNOWN-ACC");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("INR");

        when(transactionService.transfer(eq(userId), any(), any(), eq("UNKNOWN-ACC"), any(), any()))
                .thenThrow(new InvalidRecipientException("Recipient account not found: UNKNOWN-ACC"));

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Recipient account not found: UNKNOWN-ACC"));
    }

    @Test
    @DisplayName("POST /transactions/transfer insufficient balance returns 400 Bad Request")
    void transfer_InsufficientBalance_Returns400() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("ACC-202");
        request.setAmount(new BigDecimal("9999.00"));
        request.setCurrency("INR");

        when(transactionService.transfer(eq(userId), any(), any(), eq("ACC-202"), any(), any()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient balance"));
    }

    @Test
    @DisplayName("POST /transactions/transfer invalid amount (<= 0) returns 400 Bad Request")
    void transfer_InvalidAmount_Returns400() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("ACC-202");
        request.setAmount(new BigDecimal("-10.00"));
        request.setCurrency("INR");

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/transfer successful returns 201 Created")
    void transfer_Success_Returns201() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("ACC-202");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("INR");

        Transaction tx = createSampleTransaction(TransactionType.TRANSFER, new BigDecimal("50.00"));
        when(transactionService.transfer(eq(userId), any(), eq("xfer-key"), eq("ACC-202"), eq(new BigDecimal("50.00")), eq("INR")))
                .thenReturn(new TransactionResult(tx, false));
        when(transactionMapper.toResponse(tx, userId))
                .thenReturn(createSampleResponse(TransactionType.TRANSFER, new BigDecimal("50.00")));

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .header("Idempotency-Key", "xfer-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"));
    }

    @Test
    @DisplayName("POST /transactions/transfer idempotent replay returns 200 OK")
    void transfer_Replay_Returns200() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("ACC-202");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("INR");

        Transaction tx = createSampleTransaction(TransactionType.TRANSFER, new BigDecimal("50.00"));
        when(transactionService.transfer(eq(userId), any(), eq("xfer-key"), eq("ACC-202"), eq(new BigDecimal("50.00")), eq("INR")))
                .thenReturn(new TransactionResult(tx, true));
        when(transactionMapper.toResponse(tx, userId))
                .thenReturn(createSampleResponse(TransactionType.TRANSFER, new BigDecimal("50.00")));

        mockMvc.perform(post("/transactions/transfer")
                        .principal(authentication)
                        .header("Authorization", "Bearer valid-token")
                        .header("Idempotency-Key", "xfer-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"));
    }
}
