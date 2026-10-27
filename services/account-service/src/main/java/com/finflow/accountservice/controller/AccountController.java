package com.finflow.accountservice.controller;

import com.finflow.accountservice.dto.AccountResponse;
import com.finflow.accountservice.dto.DepositRequest;
import com.finflow.accountservice.dto.WithdrawRequest;
import com.finflow.accountservice.entity.Account;
import com.finflow.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        Account account = accountService.createAccount(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(account));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountId,
            Authentication authentication) {

        boolean isService = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SERVICE".equals(a.getAuthority()));

        Account account;
        if (isService) {
            account = accountService.getAccount(accountId);
        } else {
            Long userId = (Long) authentication.getPrincipal();
            account = accountService.getAccount(accountId, userId);
        }

        return ResponseEntity.ok(toResponse(account));
    }

    private AccountResponse toResponse(Account account) {

        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        Account account = accountService.getAccountByUserId(userId);

        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<AccountResponse> deposit(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        Account account = accountService.deposit(
                userId,
                request.getAmount()
        );

        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        Account account = accountService.withdraw(
                userId,
                request.getAmount()
        );

        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable String accountNumber) {
        Account account=accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/internal/{accountNumber}/deposit")
    public ResponseEntity<AccountResponse> depositToAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {

        Account account = accountService.depositToAccount(
                accountNumber,
                request.getAmount()
        );

        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/internal/refund")
    public ResponseEntity<AccountResponse> refund(
            @RequestParam Long userId,
            @Valid @RequestBody DepositRequest request) {

        Account account = accountService.refund(userId, request.getAmount());

        return ResponseEntity.ok(toResponse(account));
    }
}