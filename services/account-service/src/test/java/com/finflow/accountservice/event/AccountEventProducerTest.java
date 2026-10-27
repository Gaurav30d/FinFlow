package com.finflow.accountservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountEventProducerTest {

    @Mock
    private KafkaTemplate<String, AccountAuditEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<AccountAuditEvent> eventCaptor;

    private AccountEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AccountEventProducer(kafkaTemplate);
    }

    private AccountAuditEvent createSampleEvent(String eventType, Long actorUserId, String resourceId) {
        return AccountAuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(actorUserId)
                .resourceType("ACCOUNT")
                .resourceId(resourceId)
                .occurredAt(Instant.now())
                .outcome("SUCCESS")
                .serviceName("account-service")
                .build();
    }

    @Test
    @DisplayName("publishEvent: sends event to finflow.account.events with actorUserId as partition key")
    void publishEvent_success() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", 100L, "1234567812345678");

        CompletableFuture<SendResult<String, AccountAuditEvent>> sendFuture = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("finflow.account.events"), eq("100"), eq(event)))
                .thenReturn(sendFuture);

        CompletableFuture<SendResult<String, AccountAuditEvent>> result = producer.publishEvent(event);

        assertThat(result).isSameAs(sendFuture);
        verify(kafkaTemplate).send("finflow.account.events", "100", event);
    }

    @Test
    @DisplayName("publishEvent: skips publishing and returns completed null future when event is null")
    void publishEvent_nullEvent() {
        CompletableFuture<SendResult<String, AccountAuditEvent>> result = producer.publishEvent(null);

        assertThat(result).isNotNull();
        assertThat(result.join()).isNull();
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("derivePartitionKey: falls back to resourceId when actorUserId is null")
    void derivePartitionKey_fallbackToResourceId() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_CREATED", null, "1234567812345678");

        String key = producer.derivePartitionKey(event);

        assertThat(key).isEqualTo("1234567812345678");
    }

    @Test
    @DisplayName("derivePartitionKey: falls back to eventId when actorUserId and resourceId are null")
    void derivePartitionKey_fallbackToEventId() {
        UUID eventId = UUID.randomUUID();
        AccountAuditEvent event = AccountAuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_CREATED")
                .occurredAt(Instant.now())
                .outcome("SUCCESS")
                .build();

        String key = producer.derivePartitionKey(event);

        assertThat(key).isEqualTo(eventId.toString());
    }

    @Test
    @DisplayName("publishEvent: returns failed future when kafkaTemplate throws exception")
    void publishEvent_handlesDispatchError() {
        AccountAuditEvent event = createSampleEvent("ACCOUNT_DEPOSIT", 100L, "1234567812345678");

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka dispatch error"));

        CompletableFuture<SendResult<String, AccountAuditEvent>> result = producer.publishEvent(event);

        assertThat(result).isCompletedExceptionally();
    }
}
