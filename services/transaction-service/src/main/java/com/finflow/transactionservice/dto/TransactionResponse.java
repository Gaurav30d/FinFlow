package com.finflow.transactionservice.dto;

import com.finflow.transactionservice.entity.TransactionStatus;
import com.finflow.transactionservice.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponse {

    private UUID transactionReference;

    private TransactionType type;

    private String direction;

    private BigDecimal amount;

    private String currency;

    private String counterpartyAccount;

    private TransactionStatus status;

    private Instant createdAt;
}