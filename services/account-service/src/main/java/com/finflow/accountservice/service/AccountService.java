package com.finflow.accountservice.service;

import com.finflow.accountservice.entity.Account;
import com.finflow.accountservice.entity.AccountStatus;
import com.finflow.accountservice.exception.InsufficientBalanceException;
import com.finflow.accountservice.repository.AccountRepository;
import com.finflow.accountservice.event.AccountAuditEvent;
import com.finflow.accountservice.event.AccountEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer accountEventProducer;

    @org.springframework.transaction.annotation.Transactional
    public Account createAccount(Long userId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new RuntimeException("Account already exists for user");
        }

        try {
            Account account = Account.builder()
                    .accountNumber(generateAccountNumber())
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .currency("INR")
                    .status(AccountStatus.ACTIVE)
                    .createdAt(java.time.Instant.now())
                    .build();

            Account savedAccount = accountRepository.save(account);
            publishAccountAuditEvent("ACCOUNT_CREATED", savedAccount.getUserId(), savedAccount.getAccountNumber(), savedAccount.getCreatedAt());
            return savedAccount;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new RuntimeException("Account already exists for user", ex);
        }
    }
//
//    private String generateAccountNumber() {
//
//        return UUID.randomUUID()
//                .toString()
//                .replace("-", "")
//                .substring(0, 16);
//    }
        private String generateAccountNumber() {

            long number = 1000000000000000L
                    + (long) (Math.random() * 9000000000000000L);

            return String.valueOf(number);
        }

    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new RuntimeException("Account not found"));
    }

    public Account getAccount(Long accountId, Long authenticatedUserId) {
        Account account = getAccount(accountId);
        if (!account.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied");
        }
        return account;
    }

    public Account getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account deposit(Long userId,BigDecimal amount) {
        Account account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(()->
                        new RuntimeException("Account not found"));
        if(account.getStatus()!=AccountStatus.ACTIVE){
            throw new RuntimeException("Account status not active");
        }

        account.setBalance(account.getBalance().add(amount));

        Account savedAccount = accountRepository.save(account);
        publishAccountAuditEvent("ACCOUNT_DEPOSIT", savedAccount.getUserId(), savedAccount.getAccountNumber(), Instant.now());
        return savedAccount;
    }

    @Transactional
    public Account withdraw(Long userId,BigDecimal amount) {
        Account account=accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(()->
                        new RuntimeException("Account not found"));
        if(account.getStatus()!=AccountStatus.ACTIVE){
            throw new RuntimeException("Account status not active");
        }

        if(account.getBalance().compareTo(amount)<0){
            throw new InsufficientBalanceException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));

        Account savedAccount = accountRepository.save(account);
        publishAccountAuditEvent("ACCOUNT_WITHDRAW", savedAccount.getUserId(), savedAccount.getAccountNumber(), Instant.now());
        return savedAccount;
    }

    public Account getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account depositToAccount(String accountNumber, BigDecimal amount) {

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active");
        }

        account.setBalance(account.getBalance().add(amount));

        return accountRepository.save(account);
    }

    @Transactional
    public Account refund(Long userId,BigDecimal amount) {
        Account account=accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(account.getStatus()!=AccountStatus.ACTIVE){
            throw new RuntimeException("Account status not active");
        }

        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    private void publishAccountAuditEvent(String eventType, Long actorUserId, String accountNumber, Instant occurredAt) {
        try {
            AccountAuditEvent event = AccountAuditEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .actorUserId(actorUserId)
                    .resourceType("ACCOUNT")
                    .resourceId(accountNumber)
                    .occurredAt(occurredAt != null ? occurredAt : Instant.now())
                    .outcome("SUCCESS")
                    .correlationId(null)
                    .serviceName("account-service")
                    .build();

            accountEventProducer.publishEvent(event);
        } catch (Exception ex) {
            log.error("Failed to publish {} audit event: {}", eventType, ex.getMessage());
        }
    }
}