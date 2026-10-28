package com.finflow.authservice.event;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthEventProducerTest {

    @Mock
    private KafkaTemplate<String, AuthSecurityEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<AuthSecurityEvent> eventCaptor;

    private AuthEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AuthEventProducer(kafkaTemplate);
    }

    private AuthSecurityEvent createSampleEvent(Long actorUserId, String resourceId, UUID eventId) {
        return AuthSecurityEvent.builder()
                .eventId(eventId != null ? eventId : UUID.randomUUID())
                .eventType("USER_LOGIN_SUCCESS")
                .actorUserId(actorUserId)
                .resourceType("USER")
                .resourceId(resourceId)
                .occurredAt(Instant.now())
                .outcome("SUCCESS")
                .correlationId("trace-101")
                .build();
    }

    @Test
    @DisplayName("Sends event to finflow.auth.events with actorUserId as Kafka key when present")
    void publishEvent_WithActorUserId_SendsToCorrectTopicWithActorKey() {
        AuthSecurityEvent event = createSampleEvent(101L, "res-101", UUID.randomUUID());
        CompletableFuture<SendResult<String, AuthSecurityEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(AuthEventProducer.TOPIC), eq("101"), eq(event))).thenReturn(future);

        producer.publishEvent(event);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("finflow.auth.events");
        assertThat(keyCaptor.getValue()).isEqualTo("101");
        assertThat(eventCaptor.getValue()).isSameAs(event);
    }

    @Test
    @DisplayName("Sends event with resourceId as Kafka key when actorUserId is null")
    void publishEvent_WithoutActorUserId_UsesResourceIdAsKey() {
        AuthSecurityEvent event = createSampleEvent(null, "user@example.com", UUID.randomUUID());
        CompletableFuture<SendResult<String, AuthSecurityEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(AuthEventProducer.TOPIC), eq("user@example.com"), eq(event))).thenReturn(future);

        producer.publishEvent(event);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("finflow.auth.events");
        assertThat(keyCaptor.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Sends event with eventId as Kafka key when actor and resource are null")
    void publishEvent_WithoutActorOrResource_UsesEventIdAsKey() {
        UUID eventId = UUID.randomUUID();
        AuthSecurityEvent event = createSampleEvent(null, null, eventId);
        CompletableFuture<SendResult<String, AuthSecurityEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(AuthEventProducer.TOPIC), eq(eventId.toString()), eq(event))).thenReturn(future);

        producer.publishEvent(event);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(eventId.toString());
    }

    @Test
    @DisplayName("Derives non-null fallback key when all identifiers are null")
    void derivePartitionKey_WithoutAnyIdentifiers_GeneratesFallbackKey() {
        AuthSecurityEvent event = AuthSecurityEvent.builder().build();
        String key = producer.derivePartitionKey(event);

        assertThat(key).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Null event is skipped without calling kafkaTemplate")
    void publishEvent_NullEvent_DoesNotSend() {
        producer.publishEvent(null);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Asynchronous Kafka send success completes gracefully")
    void publishEvent_AsyncSuccess_CompletesWithoutException() {
        AuthSecurityEvent event = createSampleEvent(202L, "res-202", UUID.randomUUID());
        CompletableFuture<SendResult<String, AuthSecurityEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatCode(() -> {
            producer.publishEvent(event);
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition(AuthEventProducer.TOPIC, 0),
                    0L, 0, System.currentTimeMillis(), 0, 0
            );
            future.complete(new SendResult<>(null, metadata));
        }).doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq(AuthEventProducer.TOPIC), eq("202"), eq(event));
    }

    @Test
    @DisplayName("Asynchronous Kafka failure logs error without propagating exception to caller")
    void publishEvent_AsyncFailure_HandlesGracefullyWithoutThrowing() {
        AuthSecurityEvent event = createSampleEvent(303L, "res-303", UUID.randomUUID());
        CompletableFuture<SendResult<String, AuthSecurityEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        assertThatCode(() -> {
            producer.publishEvent(event);
            future.completeExceptionally(new KafkaException("Kafka broker unreachable"));
        }).doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq(AuthEventProducer.TOPIC), eq("303"), eq(event));
    }

    @Test
    @DisplayName("Synchronous Kafka dispatch exception is caught and does not block caller")
    void publishEvent_SyncDispatchFailure_CatchesExceptionSafely() {
        AuthSecurityEvent event = createSampleEvent(404L, "res-404", UUID.randomUUID());
        when(kafkaTemplate.send(any(), any(), any())).thenThrow(new RuntimeException("Synchronous buffer full"));

        assertThatCode(() -> producer.publishEvent(event))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq(AuthEventProducer.TOPIC), eq("404"), eq(event));
    }
}
