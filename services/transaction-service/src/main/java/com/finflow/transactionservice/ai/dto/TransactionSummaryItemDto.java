package com.finflow.transactionservice.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryItemDto {
    private UUID transactionReference;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Instant createdAt;
}
