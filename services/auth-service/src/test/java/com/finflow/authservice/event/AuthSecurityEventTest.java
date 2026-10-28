package com.finflow.authservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSecurityEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("AuthSecurityEvent builder and getters work as expected with defaults")
    void builderAndGetters_WorkWithDefaults() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        AuthSecurityEvent event = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(101L)
                .resourceType("USER")
                .resourceId("101")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("corr-123")
                .build();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(event.getActorUserId()).isEqualTo(101L);
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isEqualTo("101");
        assertThat(event.getOccurredAt()).isEqualTo(now);
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
        assertThat(event.getCorrelationId()).isEqualTo("corr-123");
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("AuthSecurityEvent serviceName always defaults to auth-service if null or blank")
    void getServiceName_DefaultsToAuthService() {
        AuthSecurityEvent event = new AuthSecurityEvent();
        assertThat(event.getServiceName()).isEqualTo("auth-service");

        event.setServiceName(null);
        assertThat(event.getServiceName()).isEqualTo("auth-service");

        event.setServiceName("   ");
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("AuthSecurityEvent strictly contains NO credential or sensitive fields")
    void classFields_ContainNoCredentialsOrSensitiveData() {
        Set<String> forbiddenSubstrings = Set.of(
                "password", "hash", "secret", "credential", "jwt",
                "refresh", "token", "payload", "authheader", "bearer"
        );

        List<Field> fields = Arrays.asList(AuthSecurityEvent.class.getDeclaredFields());
        Set<String> fieldNames = fields.stream()
                .map(Field::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String fieldName : fieldNames) {
            for (String forbidden : forbiddenSubstrings) {
                assertThat(fieldName)
                        .withFailMessage("AuthSecurityEvent must not contain sensitive field: " + fieldName)
                        .doesNotContain(forbidden);
            }
        }
    }

    @Test
    @DisplayName("AuthSecurityEvent serializes and deserializes cleanly without losing fields")
    void jsonSerialization_WorksCorrectly() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-16T12:00:00Z");

        AuthSecurityEvent original = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_FAILED")
                .actorUserId(null)
                .resourceType("USER")
                .resourceId("unknown@example.com")
                .occurredAt(now)
                .outcome("DENIED")
                .correlationId("trace-xyz")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"eventType\":\"USER_LOGIN_FAILED\"");
        assertThat(json).contains("\"outcome\":\"DENIED\"");
        assertThat(json).contains("\"serviceName\":\"auth-service\"");
        assertThat(json).doesNotContain("password");

        AuthSecurityEvent deserialized = objectMapper.readValue(json, AuthSecurityEvent.class);
        assertThat(deserialized.getEventId()).isEqualTo(eventId);
        assertThat(deserialized.getEventType()).isEqualTo("USER_LOGIN_FAILED");
        assertThat(deserialized.getActorUserId()).isNull();
        assertThat(deserialized.getResourceType()).isEqualTo("USER");
        assertThat(deserialized.getResourceId()).isEqualTo("unknown@example.com");
        assertThat(deserialized.getOccurredAt()).isEqualTo(now);
        assertThat(deserialized.getOutcome()).isEqualTo("DENIED");
        assertThat(deserialized.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("Service identity cannot be overridden in serialized event via builder, setter, or constructor")
    void serviceIdentity_CannotBeOverriddenInSerializedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        String attemptedOverride = "rogue-service";

        // 1. Attempt override via Lombok builder
        AuthSecurityEvent fromBuilder = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(1L)
                .resourceType("USER")
                .resourceId("1")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("corr-1")
                .serviceName(attemptedOverride)
                .build();

        // 2. Attempt override via setter
        AuthSecurityEvent fromSetter = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(1L)
                .resourceType("USER")
                .resourceId("1")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("corr-1")
                .build();
        fromSetter.setServiceName(attemptedOverride);

        // 3. Attempt override via all-arguments constructor
        AuthSecurityEvent fromConstructor = new AuthSecurityEvent(
                eventId,
                "USER_LOGIN_SUCCESS",
                1L,
                "USER",
                "1",
                now,
                "SUCCESS",
                "corr-1",
                attemptedOverride
        );

        List<AuthSecurityEvent> events = List.of(fromBuilder, fromSetter, fromConstructor);

        for (AuthSecurityEvent event : events) {
            assertThat(event.getServiceName()).isEqualTo(AuthSecurityEvent.DEFAULT_SERVICE_NAME);

            String serializedJson = objectMapper.writeValueAsString(event);
            JsonNode rootNode = objectMapper.readTree(serializedJson);

            assertThat(rootNode.get("serviceName").asText()).isEqualTo("auth-service");
            assertThat(serializedJson).doesNotContain(attemptedOverride);
        }
    }

    @Test
    @DisplayName("Spring Kafka JacksonJsonSerializer serializes representative event without type headers")
    void springKafkaJacksonJsonSerializer_SerializesWithoutTypeHeaders() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-21T10:00:00Z");

        AuthSecurityEvent representativeEvent = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(205L)
                .resourceType("USER")
                .resourceId("205")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("corr-audit-999")
                .serviceName("attacker-override")
                .build();

        byte[] serializedBytes;
        Headers headers = new RecordHeaders();

        try (JacksonJsonSerializer<AuthSecurityEvent> serializer = new JacksonJsonSerializer<>()) {
            serializer.configure(Map.of(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false), false);
            serializedBytes = serializer.serialize("finflow.auth.events", headers, representativeEvent);
        }

        assertThat(serializedBytes).isNotNull().isNotEmpty();

        // Assert no Kafka type headers emitted
        assertThat(headers.toArray()).isEmpty();

        // Assert valid JSON and field contents
        String jsonString = new String(serializedBytes, StandardCharsets.UTF_8);
        JsonNode rootNode = objectMapper.readTree(jsonString);

        assertThat(rootNode.isObject()).isTrue();
        assertThat(rootNode.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(rootNode.get("eventType").asText()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(rootNode.get("actorUserId").asLong()).isEqualTo(205L);
        assertThat(rootNode.get("resourceType").asText()).isEqualTo("USER");
        assertThat(rootNode.get("resourceId").asText()).isEqualTo("205");
        assertThat(rootNode.get("occurredAt").asText()).isEqualTo("2026-10-21T10:00:00Z");
        assertThat(rootNode.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(rootNode.get("correlationId").asText()).isEqualTo("corr-audit-999");
        assertThat(rootNode.get("serviceName").asText()).isEqualTo("auth-service");
        assertThat(jsonString).doesNotContain("attacker-override");
    }
}
