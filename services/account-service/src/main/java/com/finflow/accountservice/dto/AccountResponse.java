package com.finflow.accountservice.dto;

import com.finflow.accountservice.entity.AccountStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long userId;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private Instant createdAt;
}