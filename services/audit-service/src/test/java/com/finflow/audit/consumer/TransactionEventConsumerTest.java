package com.finflow.audit.consumer;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.event.TransactionCompletedEvent;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.service.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private AuditEventService auditEventService;

    @Captor
    private ArgumentCaptor<AuditEvent> auditEventCaptor;

    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionEventConsumer(auditEventService);
    }

    private TransactionCompletedEvent createValidEvent(UUID eventId, UUID transactionRef) {
        return TransactionCompletedEvent.builder()
                .eventId(eventId)
                .transactionReference(transactionRef)
                .eventType("TRANSACTION_COMPLETED")
                .senderUserId(101L)
                .receiverUserId(202L)
                .senderAccountNumber("ACC-101")
                .receiverAccountNumber("ACC-202")
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .transactionType("TRANSFER")
                .occurredAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from valid TRANSACTION_COMPLETED event")
    void consume_ValidEvent_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        UUID transactionRef = UUID.randomUUID();
        TransactionCompletedEvent event = createValidEvent(eventId, transactionRef);

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(captured.getActorUserId()).isEqualTo(101L);
        assertThat(captured.getResourceType()).isEqualTo("TRANSACTION");
        assertThat(captured.getResourceId()).isEqualTo(transactionRef.toString());
        assertThat(captured.getOccurredAt()).isEqualTo(event.getOccurredAt());
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(captured.getCorrelationId()).isEqualTo(transactionRef.toString());
        assertThat(captured.getServiceName()).isEqualTo("transaction-service");
    }

    @Test
    @DisplayName("Catches DuplicateAuditEventException and acknowledges without throwing error")
    void consume_DuplicateAuditEvent_AcknowledgesWithoutThrowing() {
        UUID eventId = UUID.randomUUID();
        UUID transactionRef = UUID.randomUUID();
        TransactionCompletedEvent event = createValidEvent(eventId, transactionRef);

        when(auditEventService.recordEvent(any()))
                .thenThrow(new DuplicateAuditEventException("Duplicate event: " + eventId));

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();

        verify(auditEventService).recordEvent(any());
    }

    @Test
    @DisplayName("Ignores unsupported event types without calling AuditEventService")
    void consume_UnsupportedEventType_IgnoredAndNeverRecorded() {
        TransactionCompletedEvent event = createValidEvent(UUID.randomUUID(), UUID.randomUUID());
        event.setEventType("TRANSACTION_INITIATED");

        consumer.consume(event);

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when payload is null")
    void consume_NullPayload_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> consumer.consume(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TransactionCompletedEvent payload cannot be null");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventType is null")
    void consume_MissingEventType_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = createValidEvent(UUID.randomUUID(), UUID.randomUUID());
        event.setEventType(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventType");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventId is null")
    void consume_MissingEventId_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = createValidEvent(null, UUID.randomUUID());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventId");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when transactionReference is null")
    void consume_MissingTransactionReference_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = createValidEvent(UUID.randomUUID(), null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required transactionReference");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when occurredAt is null")
    void consume_MissingOccurredAt_ThrowsIllegalArgumentException() {
        TransactionCompletedEvent event = createValidEvent(UUID.randomUUID(), UUID.randomUUID());
        event.setOccurredAt(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required occurredAt");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Re-throws general runtime exceptions so they reach configured Kafka error handler")
    void consume_UnexpectedServiceException_PropagatesToErrorHandler() {
        TransactionCompletedEvent event = createValidEvent(UUID.randomUUID(), UUID.randomUUID());

        when(auditEventService.recordEvent(any()))
                .thenThrow(new IllegalStateException("Database unreachable"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unreachable");

        verify(auditEventService).recordEvent(any());
    }
}
