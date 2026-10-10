package com.finflow.notificationservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TransactionCompletedEvent {

    private UUID eventId;
    private UUID transactionReference;
    private String eventType;

    private Long senderUserId;
    private Long receiverUserId;

    private String senderAccountNumber;
    private String receiverAccountNumber;

    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private Instant occurredAt;
}