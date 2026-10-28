package com.finflow.notificationservice.consumer;

import com.finflow.notificationservice.event.TransactionCompletedEvent;
import com.finflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    public static final String TOPIC = "finflow.transaction.events";
    public static final String GROUP_ID = "finflow-notification-group";

    private final NotificationService notificationService;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TransactionCompletedEvent event) {
        if (event == null) {
            log.error("Received null TransactionCompletedEvent payload");
            throw new IllegalArgumentException("TransactionCompletedEvent payload cannot be null");
        }

        validateRequiredFields(event);

        if (!"TRANSACTION_COMPLETED".equals(event.getEventType())) {
            log.info("Ignoring event with unsupported eventType: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
            return;
        }

        notificationService.processTransactionCompleted(event);
    }

    private void validateRequiredFields(TransactionCompletedEvent event) {
        if (event.getEventId() == null && event.getTransactionReference() == null) {
            log.error("TransactionCompletedEvent missing both eventId and transactionReference");
            throw new IllegalArgumentException("TransactionCompletedEvent must have at least one identifier (eventId or transactionReference)");
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            log.error("TransactionCompletedEvent missing required eventType");
            throw new IllegalArgumentException("TransactionCompletedEvent missing required eventType");
        }
        if (event.getSenderUserId() == null) {
            log.error("TransactionCompletedEvent missing required senderUserId: eventId={}", event.getEventId());
            throw new IllegalArgumentException("TransactionCompletedEvent missing required senderUserId");
        }
        if (event.getReceiverUserId() == null) {
            log.error("TransactionCompletedEvent missing required receiverUserId: eventId={}", event.getEventId());
            throw new IllegalArgumentException("TransactionCompletedEvent missing required receiverUserId");
        }
        if (event.getAmount() == null || event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("TransactionCompletedEvent missing or invalid amount: eventId={}, amount={}",
                    event.getEventId(), event.getAmount());
            throw new IllegalArgumentException("TransactionCompletedEvent missing or invalid amount");
        }
        if (event.getCurrency() == null || event.getCurrency().isBlank()) {
            log.error("TransactionCompletedEvent missing required currency: eventId={}", event.getEventId());
            throw new IllegalArgumentException("TransactionCompletedEvent missing required currency");
        }
    }
}
