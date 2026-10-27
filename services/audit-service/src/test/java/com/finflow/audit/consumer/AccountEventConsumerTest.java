package com.finflow.audit.consumer;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.event.AccountAuditEvent;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.service.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AccountEventConsumerTest {

    @Mock
    private AuditEventService auditEventService;

    @Captor
    private ArgumentCaptor<AuditEvent> auditEventCaptor;

    private AccountEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AccountEventConsumer(auditEventService);
    }

    private AccountAuditEvent createSampleEvent(String eventType, Long actorUserId, String resourceId, String outcome) {
        return AccountAuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(actorUserId)
                .resourceType("ACCOUNT")
                .resourceId(resourceId)
                .occurredAt(Instant.now())
                .outcome(outcome)
                .correlationId("trace-acc-101")
                .serviceName("account-service")
                .build();
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from ACCOUNT_CREATED event")
    void consume_ValidAccountCreatedEvent_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_CREATED")
                .actorUserId(101L)
                .resourceType("ACCOUNT")
                .resourceId("ACC-123456")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("trace-create-001")
                .serviceName("account-service")
                .build();

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(captured.getActorUserId()).isEqualTo(101L);
        assertThat(captured.getResourceType()).isEqualTo("ACCOUNT");
        assertThat(captured.getResourceId()).isEqualTo("ACC-123456");
        assertThat(captured.getOccurredAt()).isEqualTo(now);
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(captured.getCorrelationId()).isEqualTo("trace-create-001");
        assertThat(captured.getServiceName()).isEqualTo("account-service");
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from ACCOUNT_DEPOSIT event")
    void consume_ValidAccountDepositEvent_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_DEPOSIT")
                .actorUserId(101L)
                .resourceType("ACCOUNT")
                .resourceId("ACC-123456")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("trace-deposit-001")
                .serviceName("account-service")
                .build();

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("ACCOUNT_DEPOSIT");
        assertThat(captured.getActorUserId()).isEqualTo(101L);
        assertThat(captured.getResourceType()).isEqualTo("ACCOUNT");
        assertThat(captured.getResourceId()).isEqualTo("ACC-123456");
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from ACCOUNT_WITHDRAW event")
    void consume_ValidAccountWithdrawEvent_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_WITHDRAW")
                .actorUserId(101L)
                .resourceType("ACCOUNT")
                .resourceId("ACC-123456")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("trace-withdraw-001")
                .serviceName("account-service")
                .build();

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("ACCOUNT_WITHDRAW");
        assertThat(captured.getActorUserId()).isEqualTo(101L);
        assertThat(captured.getResourceType()).isEqualTo("ACCOUNT");
        assertThat(captured.getResourceId()).isEqualTo("ACC-123456");
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNT_CREATED", "ACCOUNT_DEPOSIT", "ACCOUNT_WITHDRAW"})
    @DisplayName("Supports all three standard Account audit event types")
    void consume_SupportedEventTypes_AreAllProcessed(String eventType) {
        AccountAuditEvent event = createSampleEvent(eventType, 101L, "ACC-123456", "SUCCESS");

        consumer.consume(event);

        verify(auditEventService).recordEvent(any());
    }

    @Test
    @DisplayName("Catches DuplicateAuditEventException and acknowledges without throwing error")
    void consume_DuplicateAuditEvent_AcknowledgesWithoutThrowing() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_DEPOSIT", 101L, "ACC-123456", "SUCCESS");

        when(auditEventService.recordEvent(any()))
                .thenThrow(new DuplicateAuditEventException("Duplicate event: " + event.getEventId()));

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();
        verify(auditEventService).recordEvent(any());
    }

    @Test
    @DisplayName("Ignores unsupported event types without calling AuditEventService")
    void consume_UnsupportedEventType_IgnoredAndNeverRecorded() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_FROZEN", 101L, "ACC-123456", "SUCCESS");

        consumer.consume(event);

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when payload is null")
    void consume_NullPayload_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> consumer.consume(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccountAuditEvent payload cannot be null");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventId is null")
    void consume_MissingEventId_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "SUCCESS");
        event.setEventId(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventId");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventType is null or blank")
    void consume_MissingEventType_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent(null, 101L, "ACC-123456", "SUCCESS");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventType");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when occurredAt is null")
    void consume_MissingOccurredAt_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "SUCCESS");
        event.setOccurredAt(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required occurredAt");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when resourceType is blank")
    void consume_MissingResourceType_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "SUCCESS");
        event.setResourceType("   ");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required resourceType");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when outcome is invalid")
    void consume_InvalidOutcome_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "INVALID_STATUS");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid audit outcome");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when serviceName is blank")
    void consume_MissingServiceName_ThrowsIllegalArgumentException() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "SUCCESS");
        event.setServiceName(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required serviceName");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Propagates unexpected service exception to trigger Kafka error handling / retry")
    void consume_UnexpectedServiceException_PropagatesToErrorHandler() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 101L, "ACC-123456", "SUCCESS");

        when(auditEventService.recordEvent(any()))
                .thenThrow(new IllegalStateException("Database connection pool exhausted"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database connection pool exhausted");

        verify(auditEventService).recordEvent(any());
    }
}
