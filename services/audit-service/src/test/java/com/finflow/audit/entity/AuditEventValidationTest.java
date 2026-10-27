package com.finflow.audit.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AuditEvent.AuditEventBuilder createValidAuditEventBuilder() {
        return AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("TRANSACTION_CREATED")
                .actorUserId(101L)
                .resourceType("TRANSACTION")
                .resourceId("tx-98765")
                .occurredAt(Instant.now())
                .outcome(AuditOutcome.SUCCESS)
                .correlationId("corr-12345")
                .serviceName("transaction-service");
    }

    @Test
    @DisplayName("Valid audit event should have zero constraint violations")
    void validAuditEvent_ShouldHaveNoViolations() {
        AuditEvent event = createValidAuditEventBuilder().build();
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);

        assertThat(violations).isEmpty();
        assertThat(event.getRecordedAt()).isNull();
    }

    @Test
    @DisplayName("Nullable fields actorUserId, resourceId, and correlationId can be null")
    void nullableFields_CanBeNull() {
        AuditEvent event = createValidAuditEventBuilder()
                .actorUserId(null)
                .resourceId(null)
                .correlationId(null)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Null eventId should trigger validation violation")
    void nullEventId_ShouldFailValidation() {
        AuditEvent event = createValidAuditEventBuilder()
                .eventId(null)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("eventId"));
    }

    @Test
    @DisplayName("Blank or null eventType should trigger validation violation")
    void invalidEventType_ShouldFailValidation() {
        AuditEvent eventWithNull = createValidAuditEventBuilder()
                .eventType(null)
                .build();
        assertThat(validator.validate(eventWithNull))
                .anyMatch(v -> v.getPropertyPath().toString().equals("eventType"));

        AuditEvent eventWithBlank = createValidAuditEventBuilder()
                .eventType("   ")
                .build();
        assertThat(validator.validate(eventWithBlank))
                .anyMatch(v -> v.getPropertyPath().toString().equals("eventType"));
    }

    @Test
    @DisplayName("Event type exceeding 80 characters should fail validation")
    void eventTypeExceedingMaxLength_ShouldFailValidation() {
        String longEventType = "A".repeat(81);
        AuditEvent event = createValidAuditEventBuilder()
                .eventType(longEventType)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("eventType"));
    }

    @Test
    @DisplayName("Blank or null resourceType should trigger validation violation")
    void invalidResourceType_ShouldFailValidation() {
        AuditEvent eventWithNull = createValidAuditEventBuilder()
                .resourceType(null)
                .build();
        assertThat(validator.validate(eventWithNull))
                .anyMatch(v -> v.getPropertyPath().toString().equals("resourceType"));

        AuditEvent eventWithBlank = createValidAuditEventBuilder()
                .resourceType("   ")
                .build();
        assertThat(validator.validate(eventWithBlank))
                .anyMatch(v -> v.getPropertyPath().toString().equals("resourceType"));
    }

    @Test
    @DisplayName("Resource type exceeding 50 characters should fail validation")
    void resourceTypeExceedingMaxLength_ShouldFailValidation() {
        String longResourceType = "R".repeat(51);
        AuditEvent event = createValidAuditEventBuilder()
                .resourceType(longResourceType)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("resourceType"));
    }

    @Test
    @DisplayName("Resource ID exceeding 100 characters should fail validation")
    void resourceIdExceedingMaxLength_ShouldFailValidation() {
        String longResourceId = "X".repeat(101);
        AuditEvent event = createValidAuditEventBuilder()
                .resourceId(longResourceId)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("resourceId"));
    }

    @Test
    @DisplayName("Null occurredAt should trigger validation violation")
    void nullOccurredAt_ShouldFailValidation() {
        AuditEvent event = createValidAuditEventBuilder()
                .occurredAt(null)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("occurredAt"));
    }

    @Test
    @DisplayName("Null outcome should trigger validation violation")
    void nullOutcome_ShouldFailValidation() {
        AuditEvent event = createValidAuditEventBuilder()
                .outcome(null)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("outcome"));
    }

    @Test
    @DisplayName("Correlation ID exceeding 100 characters should fail validation")
    void correlationIdExceedingMaxLength_ShouldFailValidation() {
        String longCorrelationId = "C".repeat(101);
        AuditEvent event = createValidAuditEventBuilder()
                .correlationId(longCorrelationId)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("correlationId"));
    }

    @Test
    @DisplayName("Blank or null serviceName should trigger validation violation")
    void invalidServiceName_ShouldFailValidation() {
        AuditEvent eventWithNull = createValidAuditEventBuilder()
                .serviceName(null)
                .build();
        assertThat(validator.validate(eventWithNull))
                .anyMatch(v -> v.getPropertyPath().toString().equals("serviceName"));

        AuditEvent eventWithBlank = createValidAuditEventBuilder()
                .serviceName("   ")
                .build();
        assertThat(validator.validate(eventWithBlank))
                .anyMatch(v -> v.getPropertyPath().toString().equals("serviceName"));
    }

    @Test
    @DisplayName("Service name exceeding 50 characters should fail validation")
    void serviceNameExceedingMaxLength_ShouldFailValidation() {
        String longServiceName = "S".repeat(51);
        AuditEvent event = createValidAuditEventBuilder()
                .serviceName(longServiceName)
                .build();

        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("serviceName"));
    }

    @Test
    @DisplayName("Audit event should be immutable with no public setters")
    void auditEvent_ShouldHaveNoPublicSetters() {
        List<Method> setters = Arrays.stream(AuditEvent.class.getMethods())
                .filter(method -> method.getName().startsWith("set") && method.getParameterCount() == 1)
                .toList();

        assertThat(setters).isEmpty();
    }

    @Test
    @DisplayName("PrePersist onCreate should set recordedAt if null")
    void prePersist_ShouldSetRecordedAtIfNull() throws Exception {
        AuditEvent event = createValidAuditEventBuilder()
                .recordedAt(null)
                .build();

        Method onCreateMethod = AuditEvent.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(event);

        assertThat(event.getRecordedAt()).isNotNull();
    }
}
