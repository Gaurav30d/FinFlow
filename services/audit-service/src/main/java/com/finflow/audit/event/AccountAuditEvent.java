package com.finflow.audit.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event DTO representing an account lifecycle or balance event published
 * on the Kafka topic finflow.account.events.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountAuditEvent {

    private UUID eventId;
    private String eventType;
    private Long actorUserId;
    private String resourceType;
    private String resourceId;
    private Instant occurredAt;
    private String outcome;
    private String correlationId;
    private String serviceName;
}
