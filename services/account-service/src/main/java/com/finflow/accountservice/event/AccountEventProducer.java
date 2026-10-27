package com.finflow.accountservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Producer component responsible for publishing non-sensitive account audit events
 * to the Kafka topic finflow.account.events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventProducer {

    public static final String TOPIC = "finflow.account.events";

    private final KafkaTemplate<String, AccountAuditEvent> kafkaTemplate;

    /**
     * Asynchronously publishes an audit event to Kafka.
     * Ensures non-blocking execution and safe failure handling without credential leakage.
     *
     * @param event the non-sensitive account audit event
     * @return a CompletableFuture with the send result, or a completed/failed future on immediate error
     */
    public CompletableFuture<SendResult<String, AccountAuditEvent>> publishEvent(AccountAuditEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null AccountAuditEvent, skipping");
            return CompletableFuture.completedFuture(null);
        }

        String key = derivePartitionKey(event);

        try {
            CompletableFuture<SendResult<String, AccountAuditEvent>> future =
                    kafkaTemplate.send(TOPIC, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to asynchronously publish account audit event: eventId={}, eventType={}, outcome={}, error={}",
                            event.getEventId(), event.getEventType(), event.getOutcome(), ex.getMessage());
                } else if (result != null && result.getRecordMetadata() != null) {
                    log.debug("Successfully published account audit event: eventId={}, eventType={}, topic={}, partition={}, offset={}",
                            event.getEventId(),
                            event.getEventType(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            return future;
        } catch (Exception ex) {
            log.error("Failed to dispatch account audit event to Kafka: eventId={}, eventType={}, outcome={}, error={}",
                    event.getEventId(), event.getEventType(), event.getOutcome(), ex.getMessage());
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * Derives a stable Kafka partition key.
     * Prefers actorUserId, then resourceId, then eventId, falling back to a random UUID.
     *
     * @param event the account audit event
     * @return message partition key
     */
    public String derivePartitionKey(AccountAuditEvent event) {
        if (event.getActorUserId() != null) {
            return String.valueOf(event.getActorUserId());
        }
        if (event.getResourceId() != null && !event.getResourceId().isBlank()) {
            return event.getResourceId();
        }
        if (event.getEventId() != null) {
            return event.getEventId().toString();
        }
        return UUID.randomUUID().toString();
    }
}
