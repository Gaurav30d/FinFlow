package com.finflow.audit.repository;

import com.finflow.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for immutable {@link AuditEvent} entities.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Checks whether an audit event with the given unique business eventId already exists.
     *
     * @param eventId the unique event identifier
     * @return true if an audit event exists with the given eventId, false otherwise
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Finds an audit event by its unique business eventId.
     *
     * @param eventId the unique event identifier
     * @return an Optional containing the audit event if found
     */
    Optional<AuditEvent> findByEventId(UUID eventId);
}
