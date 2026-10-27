package com.finflow.audit.service;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.repository.AuditEventRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for validating and persisting immutable audit events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final Validator validator;

    /**
     * Validates and persists an immutable audit event.
     *
     * @param auditEvent the audit event to record
     * @return the persisted audit event
     * @throws IllegalArgumentException       if auditEvent is null
     * @throws ConstraintViolationException   if Bean Validation fails
     * @throws DuplicateAuditEventException    if an audit event with the same eventId already exists
     * @throws DataIntegrityViolationException if any other database integrity violation occurs
     */
    @Transactional
    public AuditEvent recordEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            throw new IllegalArgumentException("Audit event cannot be null");
        }

        // Programmatic Bean Validation before hitting the database
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(auditEvent);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Audit event validation failed", violations);
        }

        UUID eventId = auditEvent.getEventId();

        // Fast-path existence check (mitigates common duplicates)
        if (auditEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate audit event detected during existence check: eventId={}", eventId);
            throw new DuplicateAuditEventException("Audit event with eventId " + eventId + " already exists");
        }

        try {
            // Flush immediately to force unique constraint enforcement within this transaction block
            AuditEvent savedEvent = auditEventRepository.saveAndFlush(auditEvent);
            log.info("Recorded audit event: id={}, eventId={}, eventType={}",
                    savedEvent.getId(), savedEvent.getEventId(), savedEvent.getEventType());
            return savedEvent;
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateEventIdViolation(ex)) {
                log.warn("Database unique constraint violation for duplicate eventId={}", eventId);
                throw new DuplicateAuditEventException(
                        "Audit event with eventId " + eventId + " already exists", ex);
            }
            // Preserve other database integrity violations (foreign keys, nulls, truncation, etc.)
            throw ex;
        }
    }

    /**
     * Looks up an audit event by its business event ID.
     *
     * @param eventId the event ID
     * @return an Optional containing the audit event if found
     */
    @Transactional(readOnly = true)
    public Optional<AuditEvent> findByEventId(UUID eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        return auditEventRepository.findByEventId(eventId);
    }

    /**
     * Inspects the exception cause hierarchy to verify if the violation is specifically
     * caused by the unique constraint on event_id.
     */
    private boolean isDuplicateEventIdViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getMessage();
        if (rootMsg != null) {
            String lower = rootMsg.toLowerCase();
            if (lower.contains("uk_audit_events_event_id") ||
                (lower.contains("duplicate") && lower.contains("event_id"))) {
                return true;
            }
        }

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null) {
                    String lower = constraintName.toLowerCase();
                    if (lower.contains("uk_audit_events_event_id") || lower.contains("event_id")) {
                        return true;
                    }
                }
            }
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("uk_audit_events_event_id") ||
                    (lower.contains("duplicate entry") && lower.contains("event_id")) ||
                    (lower.contains("duplicate") && lower.contains("key") && lower.contains("event_id"))) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
