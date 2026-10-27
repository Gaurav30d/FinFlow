package com.finflow.audit.consumer;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.event.AuthSecurityEvent;
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
class AuthEventConsumerTest {

    @Mock
    private AuditEventService auditEventService;

    @Captor
    private ArgumentCaptor<AuditEvent> auditEventCaptor;

    private AuthEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthEventConsumer(auditEventService);
    }

    private AuthSecurityEvent createSampleEvent(String eventType, Long actorUserId, String resourceId, String outcome) {
        return AuthSecurityEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(actorUserId)
                .resourceType("USER")
                .resourceId(resourceId)
                .occurredAt(Instant.now())
                .outcome(outcome)
                .correlationId("trace-auth-001")
                .serviceName("auth-service")
                .build();
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from USER_LOGIN_SUCCESS event")
    void consume_ValidSuccessEvent_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AuthSecurityEvent event = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(105L)
                .resourceType("USER")
                .resourceId("105")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("trace-login-105")
                .serviceName("auth-service")
                .build();

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(captured.getActorUserId()).isEqualTo(105L);
        assertThat(captured.getResourceType()).isEqualTo("USER");
        assertThat(captured.getResourceId()).isEqualTo("105");
        assertThat(captured.getOccurredAt()).isEqualTo(now);
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(captured.getCorrelationId()).isEqualTo("trace-login-105");
        assertThat(captured.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("Successfully maps and records AuditEvent from USER_REGISTER_FAILURE with null user identifiers")
    void consume_ValidFailureEventWithNullIdentifiers_SuccessfullyMapsAndRecordsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AuthSecurityEvent event = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_REGISTER_FAILURE")
                .actorUserId(null)
                .resourceType("USER")
                .resourceId(null)
                .occurredAt(now)
                .outcome("FAILURE")
                .correlationId(null)
                .serviceName("auth-service")
                .build();

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        AuditEvent captured = auditEventCaptor.getValue();

        assertThat(captured.getEventId()).isEqualTo(eventId);
        assertThat(captured.getEventType()).isEqualTo("USER_REGISTER_FAILURE");
        assertThat(captured.getActorUserId()).isNull();
        assertThat(captured.getResourceType()).isEqualTo("USER");
        assertThat(captured.getResourceId()).isNull();
        assertThat(captured.getOccurredAt()).isEqualTo(now);
        assertThat(captured.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(captured.getCorrelationId()).isNull();
        assertThat(captured.getServiceName()).isEqualTo("auth-service");
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER_REGISTER_SUCCESS", "USER_REGISTER_FAILURE", "USER_LOGIN_SUCCESS", "USER_LOGIN_FAILURE"})
    @DisplayName("Supports all four standard Auth security event types")
    void consume_SupportedEventTypes_AreAllProcessed(String eventType) {
        String outcome = eventType.endsWith("SUCCESS") ? "SUCCESS" : "FAILURE";
        AuthSecurityEvent event = createSampleEvent(eventType, 201L, "201", outcome);

        consumer.consume(event);

        verify(auditEventService).recordEvent(any());
    }

    @Test
    @DisplayName("Successfully maps DENIED outcome to AuditOutcome.DENIED")
    void consume_DeniedOutcome_MapsToAuditOutcomeDenied() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_FAILURE", null, null, "DENIED");

        consumer.consume(event);

        verify(auditEventService).recordEvent(auditEventCaptor.capture());
        assertThat(auditEventCaptor.getValue().getOutcome()).isEqualTo(AuditOutcome.DENIED);
    }

    @Test
    @DisplayName("Catches DuplicateAuditEventException and acknowledges without throwing error")
    void consume_DuplicateAuditEvent_AcknowledgesWithoutThrowing() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");

        when(auditEventService.recordEvent(any()))
                .thenThrow(new DuplicateAuditEventException("Duplicate event: " + event.getEventId()));

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();
        verify(auditEventService).recordEvent(any());
    }

    @Test
    @DisplayName("Ignores unsupported event types without calling AuditEventService")
    void consume_UnsupportedEventType_IgnoredAndNeverRecorded() {
        AuthSecurityEvent event = createSampleEvent("PASSWORD_CHANGED_SUCCESS", 101L, "101", "SUCCESS");

        consumer.consume(event);

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when payload is null")
    void consume_NullPayload_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> consumer.consume(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AuthSecurityEvent payload cannot be null");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventId is null")
    void consume_MissingEventId_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");
        event.setEventId(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventId");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when eventType is null or blank")
    void consume_MissingEventType_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent(null, 101L, "101", "SUCCESS");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required eventType");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when occurredAt is null")
    void consume_MissingOccurredAt_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");
        event.setOccurredAt(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required occurredAt");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when resourceType is blank")
    void consume_MissingResourceType_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");
        event.setResourceType("   ");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required resourceType");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when outcome is invalid")
    void consume_InvalidOutcome_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "INVALID_STATUS");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid audit outcome");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when serviceName is blank")
    void consume_MissingServiceName_ThrowsIllegalArgumentException() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");
        event.setServiceName(null);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required serviceName");

        verify(auditEventService, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Propagates unexpected service exception to trigger Kafka error handling / retry")
    void consume_UnexpectedServiceException_PropagatesToErrorHandler() {
        AuthSecurityEvent event = createSampleEvent("USER_LOGIN_SUCCESS", 101L, "101", "SUCCESS");

        when(auditEventService.recordEvent(any()))
                .thenThrow(new IllegalStateException("Database connection pool exhausted"));

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database connection pool exhausted");

        verify(auditEventService).recordEvent(any());
    }
}
