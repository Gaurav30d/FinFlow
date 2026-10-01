package com.finflow.transactionservice.controller;

import com.finflow.transactionservice.client.AccountClient;
import com.finflow.transactionservice.dto.CreateTransactionRequest;
import com.finflow.transactionservice.dto.TransactionResponse;
import com.finflow.transactionservice.dto.TransferRequest;
import com.finflow.transactionservice.entity.Transaction;
import com.finflow.transactionservice.mapper.TransactionMapper;
import com.finflow.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.finflow.transactionservice.dto.DepositRequest;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final AccountClient accountClient;
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(Authentication authentication,
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateTransactionRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        Transaction transaction =
                transactionService.createTransaction(
                        userId,
                        token,
                        request.getType(),
                        request.getAmount(),
                        request.getCurrency()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }


    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        List<TransactionResponse> transactions =
                transactionService
                        .getTransactionsByUserId(userId)
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = (Long) authentication.getPrincipal();

        Transaction transaction =
                transactionService.transfer(
                        userId,
                        token,
                        request.getReceiverAccountNumber(),
                        request.getAmount(),
                        request.getCurrency()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }

}