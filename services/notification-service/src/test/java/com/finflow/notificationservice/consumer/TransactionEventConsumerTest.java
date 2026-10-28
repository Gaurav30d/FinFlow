package com.finflow.notificationservice.consumer;

import com.finflow.notificationservice.event.TransactionCompletedEvent;
import com.finflow.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionEventConsumer(notificationService);
    }

    private TransactionCompletedEvent.TransactionCompletedEventBuilder validEventBuilder() {
        return TransactionCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .transactionReference(UUID.randomUUID())
                .eventType("TRANSACTION_COMPLETED")
                .senderUserId(101L)
                .receiverUserId(202L)
                .senderAccountNumber("ACC10001")
                .receiverAccountNumber("ACC20002")
                .amount(new BigDecimal("250.00"))
                .currency("INR")
                .transactionType("TRANSFER")
                .occurredAt(Instant.now());
    }

    @Test
    @DisplayName("Valid TRANSACTION_COMPLETED event is processed by NotificationService")
    void consume_ValidEvent_ProcessesSuccessfully() {
        TransactionCompletedEvent event = validEventBuilder().build();

        consumer.consume(event);

        verify(notificationService).processTransactionCompleted(event);
    }

    @Test
    @DisplayName("Valid event with null eventId falls back to transactionReference identifier")
    void consume_NullEventId_FallsBackToTransactionReference() {
        TransactionCompletedEvent event = validEventBuilder()
                .eventId(null)
                .transactionReference(UUID.randomUUID())
                .build();

        consumer.consume(event);

        verify(notificationService).processTransactionCompleted(event);
    }

    @Test
    @DisplayName("Null event payload throws IllegalArgumentException")
    void consume_NullEvent_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> consumer.consume(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TransactionCompletedEvent payload cannot be null");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event missing both eventId and transactionReference throws IllegalArgumentException")
    void consume_MissingBothIdentifiers_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = validEventBuilder()
                .eventId(null)
                .transactionReference(null)
                .build();

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one identifier");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event missing eventType throws IllegalArgumentException")
    void consume_MissingEventType_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = validEventBuilder()
                .eventType(null)
                .build();

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventType");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event missing senderUserId throws IllegalArgumentException")
    void consume_MissingSenderUserId_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = validEventBuilder()
                .senderUserId(null)
                .build();

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required senderUserId");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event missing receiverUserId throws IllegalArgumentException")
    void consume_MissingReceiverUserId_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = validEventBuilder()
                .receiverUserId(null)
                .build();

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required receiverUserId");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event with missing or negative amount throws IllegalArgumentException")
    void consume_InvalidAmount_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent zeroAmount = validEventBuilder()
                .amount(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> consumer.consume(zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing or invalid amount");

        TransactionCompletedEvent negativeAmount = validEventBuilder()
                .amount(new BigDecimal("-10.00"))
                .build();

        assertThatThrownBy(() -> consumer.consume(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing or invalid amount");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Event missing currency throws IllegalArgumentException")
    void consume_MissingCurrency_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = validEventBuilder()
                .currency("  ")
                .build();

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required currency");

        verify(notificationService, never()).processTransactionCompleted(any());
    }

    @Test
    @DisplayName("Unsupported eventType is ignored safely without throwing error")
    void consume_UnsupportedEventType_IgnoredSafely() {
        TransactionCompletedEvent event = validEventBuilder()
                .eventType("TRANSACTION_INITIATED")
                .build();

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();

        verify(notificationService, never()).processTransactionCompleted(any());
    }
}
