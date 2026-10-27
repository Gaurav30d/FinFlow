package com.finflow.accountservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountAuditEventTest {

    @Test
    @DisplayName("serviceName is always enforced as 'account-service' regardless of builder value")
    void serviceName_enforcedInBuilder() {
        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ACCOUNT_CREATED")
                .actorUserId(100L)
                .resourceType("ACCOUNT")
                .resourceId("1234567812345678")
                .occurredAt(Instant.now())
                .outcome("SUCCESS")
                .serviceName("malicious-service")
                .build();

        assertThat(event.getServiceName()).isEqualTo("account-service");
    }

    @Test
    @DisplayName("serviceName is always enforced as 'account-service' on setter invocation")
    void serviceName_enforcedInSetter() {
        AccountAuditEvent event = new AccountAuditEvent();
        event.setServiceName("forged-service");

        assertThat(event.getServiceName()).isEqualTo("account-service");
    }

    @Test
    @DisplayName("Serializes accurately with Spring Kafka JacksonJsonSerializer")
    void serialize_withJacksonJsonSerializer() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-26T10:00:00Z");

        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_DEPOSIT")
                .actorUserId(100L)
                .resourceType("ACCOUNT")
                .resourceId("1234567812345678")
                .occurredAt(now)
                .outcome("SUCCESS")
                .correlationId("test-corr-123")
                .build();

        JacksonJsonSerializer<AccountAuditEvent> serializer = new JacksonJsonSerializer<>();
        byte[] bytes = serializer.serialize("finflow.account.events", event);
        serializer.close();

        assertThat(bytes).isNotNull();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JsonNode json = mapper.readTree(bytes);

        assertThat(json.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(json.get("eventType").asText()).isEqualTo("ACCOUNT_DEPOSIT");
        assertThat(json.get("actorUserId").asLong()).isEqualTo(100L);
        assertThat(json.get("resourceType").asText()).isEqualTo("ACCOUNT");
        assertThat(json.get("resourceId").asText()).isEqualTo("1234567812345678");
        assertThat(json.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(json.get("serviceName").asText()).isEqualTo("account-service");
        assertThat(json.get("correlationId").asText()).isEqualTo("test-corr-123");

        // Verify absence of sensitive data
        assertThat(json.has("password")).isFalse();
        assertThat(json.has("token")).isFalse();
        assertThat(json.has("secret")).isFalse();
    }
}
