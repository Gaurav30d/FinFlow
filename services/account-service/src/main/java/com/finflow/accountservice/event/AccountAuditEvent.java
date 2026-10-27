package com.finflow.accountservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Strict immutable DTO representing an account lifecycle or financial event published to Kafka.
 * Contains only necessary non-sensitive audit metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AccountAuditEvent {

    public static final String DEFAULT_SERVICE_NAME = "account-service";

    private UUID eventId;
    private String eventType;
    private Long actorUserId;
    private String resourceType;
    private String resourceId;
    private Instant occurredAt;
    private String outcome;
    private String correlationId;

    @Builder.Default
    private String serviceName = DEFAULT_SERVICE_NAME;

    public void setServiceName(String serviceName) {
        this.serviceName = DEFAULT_SERVICE_NAME;
    }

    public String getServiceName() {
        return DEFAULT_SERVICE_NAME;
    }
}
