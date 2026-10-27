package com.finflow.audit.consumer;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.event.AccountAuditEvent;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Kafka consumer responsible for ingesting account lifecycle and balance events
 * from finflow.account.events and recording audit log entries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    public static final String TOPIC = "finflow.account.events";
    public static final String GROUP_ID = "finflow-audit-account-group";

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "ACCOUNT_CREATED",
            "ACCOUNT_DEPOSIT",
            "ACCOUNT_WITHDRAW"
    );

    private final AuditEventService auditEventService;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            containerFactory = "accountKafkaListenerContainerFactory"
    )
    public void consume(AccountAuditEvent event) {
        if (event == null) {
            log.error("Received null AccountAuditEvent payload");
            throw new IllegalArgumentException("AccountAuditEvent payload cannot be null");
        }

        validateRequiredFields(event);

        if (!SUPPORTED_EVENT_TYPES.contains(event.getEventType())) {
            log.info("Ignoring event with unsupported eventType: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
            return;
        }

        AuditOutcome auditOutcome;
        try {
            auditOutcome = AuditOutcome.valueOf(event.getOutcome().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.error("AccountAuditEvent has invalid outcome: eventId={}, outcome={}",
                    event.getEventId(), event.getOutcome());
            throw new IllegalArgumentException("Invalid audit outcome: " + event.getOutcome());
        }

        AuditEvent auditEvent = AuditEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .actorUserId(event.getActorUserId())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .occurredAt(event.getOccurredAt())
                .outcome(auditOutcome)
                .correlationId(event.getCorrelationId())
                .serviceName(event.getServiceName())
                .build();

        try {
            auditEventService.recordEvent(auditEvent);
            log.info("Successfully audited account event: eventId={}, eventType={}, outcome={}",
                    event.getEventId(), event.getEventType(), auditOutcome);
        } catch (DuplicateAuditEventException ex) {
            log.info("Account audit event already processed, acknowledging duplicate without retrying: eventId={}",
                    event.getEventId());
        }
    }

    private void validateRequiredFields(AccountAuditEvent event) {
        if (event.getEventId() == null) {
            log.error("AccountAuditEvent missing required eventId");
            throw new IllegalArgumentException("AccountAuditEvent missing required eventId");
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            log.error("AccountAuditEvent missing required eventType: eventId={}", event.getEventId());
            throw new IllegalArgumentException("AccountAuditEvent missing required eventType");
        }
        if (event.getOccurredAt() == null) {
            log.error("AccountAuditEvent missing required occurredAt: eventId={}", event.getEventId());
            throw new IllegalArgumentException("AccountAuditEvent missing required occurredAt");
        }
        if (event.getResourceType() == null || event.getResourceType().isBlank()) {
            log.error("AccountAuditEvent missing required resourceType: eventId={}", event.getEventId());
            throw new IllegalArgumentException("AccountAuditEvent missing required resourceType");
        }
        if (event.getOutcome() == null || event.getOutcome().isBlank()) {
            log.error("AccountAuditEvent missing required outcome: eventId={}", event.getEventId());
            throw new IllegalArgumentException("AccountAuditEvent missing required outcome");
        }
        if (event.getServiceName() == null || event.getServiceName().isBlank()) {
            log.error("AccountAuditEvent missing required serviceName: eventId={}", event.getEventId());
            throw new IllegalArgumentException("AccountAuditEvent missing required serviceName");
        }
    }
}
