package com.finflow.accountservice.service;

import com.finflow.accountservice.entity.Account;
import com.finflow.accountservice.entity.AccountStatus;
import com.finflow.accountservice.exception.InsufficientBalanceException;
import com.finflow.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account createAccount(Long userId) {

        if (accountRepository.existsByUserId(userId)) {
            throw new RuntimeException("Account already exists for user");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();

        return accountRepository.save(account);
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
                .orElseThrow(()->
                        new RuntimeException("Account not found "));
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

        return accountRepository.save(account);
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

        return accountRepository.save(account);
    }

    public Account getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account depositToAccount(String accountNumber, BigDecimal amount) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
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
}