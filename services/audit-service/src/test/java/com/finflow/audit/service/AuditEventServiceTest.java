package com.finflow.audit.service;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.repository.AuditEventRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private Validator validator;
    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        auditEventService = new AuditEventService(auditEventRepository, validator);
    }

    private AuditEvent createValidAuditEvent(UUID eventId) {
        return AuditEvent.builder()
                .eventId(eventId)
                .eventType("TRANSFER_INITIATED")
                .actorUserId(555L)
                .resourceType("TRANSFER")
                .resourceId("tx-12345")
                .occurredAt(Instant.now())
                .outcome(AuditOutcome.SUCCESS)
                .correlationId("corr-999")
                .serviceName("transaction-service")
                .build();
    }

    @Test
    @DisplayName("Successfully validates and persists a valid audit event")
    void recordEvent_ValidEvent_SuccessfullyPersists() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = createValidAuditEvent(eventId);

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(auditEventRepository.saveAndFlush(event)).thenReturn(event);

        AuditEvent result = auditEventService.recordEvent(event);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo(eventId);
        verify(auditEventRepository).existsByEventId(eventId);
        verify(auditEventRepository).saveAndFlush(event);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when audit event is null without touching database")
    void recordEvent_NullEvent_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> auditEventService.recordEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audit event cannot be null");

        verify(auditEventRepository, never()).existsByEventId(any());
        verify(auditEventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Enforces Bean Validation and throws ConstraintViolationException before database access")
    void recordEvent_InvalidEvent_ThrowsConstraintViolationExceptionBeforeDatabase() {
        AuditEvent invalidEvent = AuditEvent.builder()
                .eventId(null) // Violation: eventId is required
                .eventType("") // Violation: eventType is required
                .resourceType("TRANSFER")
                .occurredAt(Instant.now())
                .outcome(AuditOutcome.SUCCESS)
                .serviceName("transaction-service")
                .build();

        assertThatThrownBy(() -> auditEventService.recordEvent(invalidEvent))
                .isInstanceOf(ConstraintViolationException.class);

        verify(auditEventRepository, never()).existsByEventId(any());
        verify(auditEventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Throws DuplicateAuditEventException when existsByEventId returns true")
    void recordEvent_ExistingEventId_ThrowsDuplicateAuditEventException() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = createValidAuditEvent(eventId);

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(true);

        assertThatThrownBy(() -> auditEventService.recordEvent(event))
                .isInstanceOf(DuplicateAuditEventException.class)
                .hasMessageContaining(eventId.toString());

        verify(auditEventRepository).existsByEventId(eventId);
        verify(auditEventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Preserves database unique constraint as final barrier when concurrent race occurs")
    void recordEvent_ConcurrentRace_ThrowsDuplicateAuditEventExceptionOnUniqueConstraintViolation() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = createValidAuditEvent(eventId);

        // Simulate concurrent requests both seeing existsByEventId == false
        when(auditEventRepository.existsByEventId(eventId)).thenReturn(false);

        // Simulate database unique constraint violation from flush
        SQLException sqlException = new SQLException("Duplicate entry for key 'uk_audit_events_event_id'", "23000", 1062);
        org.hibernate.exception.ConstraintViolationException hibernateCve =
                new org.hibernate.exception.ConstraintViolationException("could not execute statement", sqlException, "INSERT INTO audit_events...", "uk_audit_events_event_id");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Unique constraint violation", hibernateCve);

        when(auditEventRepository.saveAndFlush(event)).thenThrow(dive);

        assertThatThrownBy(() -> auditEventService.recordEvent(event))
                .isInstanceOf(DuplicateAuditEventException.class)
                .hasMessageContaining(eventId.toString())
                .hasCause(dive);

        verify(auditEventRepository).existsByEventId(eventId);
        verify(auditEventRepository).saveAndFlush(event);
    }

    @Test
    @DisplayName("Preserves unrelated database integrity errors without masking as duplicate event")
    void recordEvent_OtherDataIntegrityError_RethrowsOriginalException() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = createValidAuditEvent(eventId);

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(false);

        SQLException sqlException = new SQLException("Data truncation: Data too long for column", "22001", 1406);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Data truncation", sqlException);

        when(auditEventRepository.saveAndFlush(event)).thenThrow(dive);

        assertThatThrownBy(() -> auditEventService.recordEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateAuditEventException.class)
                .hasMessageContaining("Data truncation");

        verify(auditEventRepository).saveAndFlush(event);
    }

    @Test
    @DisplayName("Looks up event by business event ID")
    void findByEventId_ExistingId_ReturnsAuditEvent() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = createValidAuditEvent(eventId);

        when(auditEventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));

        Optional<AuditEvent> result = auditEventService.findByEventId(eventId);

        assertThat(result).isPresent().contains(event);
        verify(auditEventRepository).findByEventId(eventId);
    }

    @Test
    @DisplayName("findByEventId with null returns empty without querying repository")
    void findByEventId_NullId_ReturnsEmpty() {
        Optional<AuditEvent> result = auditEventService.findByEventId(null);

        assertThat(result).isEmpty();
        verify(auditEventRepository, never()).findByEventId(any());
    }
}
