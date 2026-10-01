package com.finflow.transactionservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long userId;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant createdAt;
}