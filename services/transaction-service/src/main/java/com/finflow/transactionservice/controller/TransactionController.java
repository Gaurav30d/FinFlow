package com.finflow.transactionservice.controller;

import com.finflow.transactionservice.dto.CreateTransactionRequest;
import com.finflow.transactionservice.dto.TransactionResponse;
import com.finflow.transactionservice.dto.TransactionResult;
import com.finflow.transactionservice.dto.TransferRequest;
import com.finflow.transactionservice.mapper.TransactionMapper;
import com.finflow.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication authentication,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        TransactionResult result =
                transactionService.createTransaction(
                        userId,
                        token,
                        idempotencyKey,
                        request.getType(),
                        request.getAmount(),
                        request.getCurrency()
                );

        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .body(transactionMapper.toResponse(result.transaction(), userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        List<TransactionResponse> transactions =
                transactionService
                        .getTransactionsByUserId(userId)
                        .stream()
                        .map(transaction ->
                                transactionMapper.toResponse(
                                        transaction,
                                        userId
                                )
                        )
                        .toList();

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            Authentication authentication,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        TransactionResult result =
                transactionService.transfer(
                        userId,
                        token,
                        idempotencyKey,
                        request.getReceiverAccountNumber(),
                        request.getAmount(),
                        request.getCurrency()
                );

        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .body(transactionMapper.toResponse(result.transaction(), userId));
    }
}