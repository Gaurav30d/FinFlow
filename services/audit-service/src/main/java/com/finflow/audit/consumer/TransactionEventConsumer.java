package com.finflow.audit.consumer;

import com.finflow.audit.entity.AuditEvent;
import com.finflow.audit.entity.AuditOutcome;
import com.finflow.audit.event.TransactionCompletedEvent;
import com.finflow.audit.exception.DuplicateAuditEventException;
import com.finflow.audit.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer responsible for ingesting transaction lifecycle events and recording
 * audit log entries in the FinFlow Audit Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    public static final String TOPIC = "finflow.transaction.events";
    public static final String GROUP_ID = "finflow-audit-group";
    private static final String EXPECTED_EVENT_TYPE = "TRANSACTION_COMPLETED";
    private static final String RESOURCE_TYPE_TRANSACTION = "TRANSACTION";
    private static final String SERVICE_NAME_TRANSACTION = "transaction-service";

    private final AuditEventService auditEventService;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID
    )
    public void consume(TransactionCompletedEvent event) {
        if (event == null) {
            log.error("Received null TransactionCompletedEvent payload");
            throw new IllegalArgumentException("TransactionCompletedEvent payload cannot be null");
        }

        validateRequiredFields(event);

        String eventType = event.getEventType();
        if (!EXPECTED_EVENT_TYPE.equals(eventType)) {
            log.info("Ignoring event with unsupported eventType: {}", eventType);
            return;
        }

        AuditEvent auditEvent = AuditEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .actorUserId(event.getSenderUserId())
                .resourceType(RESOURCE_TYPE_TRANSACTION)
                .resourceId(event.getTransactionReference().toString())
                .occurredAt(event.getOccurredAt())
                .outcome(AuditOutcome.SUCCESS)
                .correlationId(event.getTransactionReference().toString())
                .serviceName(SERVICE_NAME_TRANSACTION)
                .build();

        try {
            auditEventService.recordEvent(auditEvent);
            log.info("Successfully audited transaction event: eventId={}, transactionRef={}",
                    event.getEventId(), event.getTransactionReference());
        } catch (DuplicateAuditEventException ex) {
            log.info("Audit event already processed, acknowledging duplicate without retrying: eventId={}",
                    event.getEventId());
        }
    }

    private void validateRequiredFields(TransactionCompletedEvent event) {
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            log.error("TransactionCompletedEvent missing required eventType");
            throw new IllegalArgumentException("TransactionCompletedEvent missing required eventType");
        }
        if (event.getEventId() == null) {
            log.error("TransactionCompletedEvent missing required eventId");
            throw new IllegalArgumentException("TransactionCompletedEvent missing required eventId");
        }
        if (event.getTransactionReference() == null) {
            log.error("TransactionCompletedEvent missing required transactionReference: eventId={}", event.getEventId());
            throw new IllegalArgumentException("TransactionCompletedEvent missing required transactionReference");
        }
        if (event.getOccurredAt() == null) {
            log.error("TransactionCompletedEvent missing required occurredAt: eventId={}", event.getEventId());
            throw new IllegalArgumentException("TransactionCompletedEvent missing required occurredAt");
        }
    }
}
