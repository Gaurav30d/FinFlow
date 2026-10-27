package com.finflow.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit record representing an event that occurred within FinFlow.
 */
@Entity
@Table(
    name = "audit_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_audit_events_event_id", columnNames = "event_id")
    },
    indexes = {
        @Index(name = "idx_audit_events_event_id", columnList = "event_id"),
        @Index(name = "idx_audit_events_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_events_actor_user_id", columnList = "actor_user_id"),
        @Index(name = "idx_audit_events_resource", columnList = "resource_type, resource_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Event ID is required")
    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @NotBlank(message = "Event type is required")
    @Size(max = 80, message = "Event type cannot exceed 80 characters")
    @Column(name = "event_type", nullable = false, length = 80, updatable = false)
    private String eventType;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Resource type cannot exceed 50 characters")
    @Column(name = "resource_type", nullable = false, length = 50, updatable = false)
    private String resourceType;

    @Size(max = 100, message = "Resource ID cannot exceed 100 characters")
    @Column(name = "resource_id", length = 100, updatable = false)
    private String resourceId;

    @NotNull(message = "Occurred at timestamp is required")
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @NotNull(message = "Outcome is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20, updatable = false)
    private AuditOutcome outcome;

    @Size(max = 100, message = "Correlation ID cannot exceed 100 characters")
    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @NotBlank(message = "Service name is required")
    @Size(max = 50, message = "Service name cannot exceed 50 characters")
    @Column(name = "service_name", nullable = false, length = 50, updatable = false)
    private String serviceName;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditEvent that = (AuditEvent) o;
        return eventId != null && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
