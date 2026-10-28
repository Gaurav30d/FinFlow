package com.finflow.authservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finflow.authservice.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated live integration test for the Auth Service Kafka producer.
 * Exercises actual live publishing to the finflow.auth.events topic on localhost:9092.
 * Guarded with @EnabledIfSystemProperty so that standard unit tests run without a running broker.
 */
@SpringJUnitConfig(classes = {KafkaConfig.class, AuthEventProducer.class})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092"
})
@EnabledIfSystemProperty(named = "live.kafka.test", matches = "true")
class AuthEventProducerLiveTest {

    @Autowired
    private AuthEventProducer producer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Publishes synthetic USER_LOGIN_SUCCESS event to live Kafka and verifies consumed message")
    void publishSyntheticEvent_LiveKafka_SucceedsAndConsumesWithoutTypeHeaders() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String correlationId = "corr-live-" + UUID.randomUUID();
        Long actorUserId = 99999L;
        String resourceType = "USER";
        String resourceId = "synthetic-test-user";
        String eventType = "USER_LOGIN_SUCCESS";
        String outcome = "SUCCESS";

        AuthSecurityEvent syntheticEvent = AuthSecurityEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .actorUserId(actorUserId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .occurredAt(occurredAt)
                .outcome(outcome)
                .correlationId(correlationId)
                .serviceName("attempted-override")
                .build();

        // 1. Prepare ephemeral Kafka consumer subscribed to finflow.auth.events
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-live-test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            List<TopicPartition> partitions = List.of(
                    new TopicPartition(AuthEventProducer.TOPIC, 0),
                    new TopicPartition(AuthEventProducer.TOPIC, 1),
                    new TopicPartition(AuthEventProducer.TOPIC, 2)
            );
            consumer.assign(partitions);

            // 2. Publish synthetic event and wait for broker acknowledgment within 5 seconds
            CompletableFuture<SendResult<String, AuthSecurityEvent>> future = producer.publishEvent(syntheticEvent);
            SendResult<String, AuthSecurityEvent> sendResult = future.get(5, TimeUnit.SECONDS);

            assertThat(sendResult).isNotNull();
            assertThat(sendResult.getRecordMetadata()).isNotNull();
            assertThat(sendResult.getRecordMetadata().topic()).isEqualTo(AuthEventProducer.TOPIC);
            assertThat(sendResult.getProducerRecord().key()).isEqualTo(String.valueOf(actorUserId));

            // 3. Poll for the matching record using bounded deadline
            ConsumerRecord<String, String> matchedRecord = null;
            long deadline = System.currentTimeMillis() + 8_000;

            while (System.currentTimeMillis() < deadline && matchedRecord == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() != null && record.value().contains(eventId.toString())) {
                        matchedRecord = record;
                        break;
                    }
                }
            }

            assertThat(matchedRecord)
                    .withFailMessage("Timed out waiting to consume event with eventId=" + eventId)
                    .isNotNull();

            // 4. Verify Kafka type metadata headers are absent
            for (Header header : matchedRecord.headers()) {
                String key = header.key();
                assertThat(key)
                        .withFailMessage("Kafka record should not contain type header: " + key)
                        .doesNotContainIgnoringCase("type")
                        .isNotEqualTo("__TypeId__")
                        .isNotEqualTo("spring.json.header.types");
            }
            assertThat(matchedRecord.headers().toArray()).isEmpty();

            // 5. Verify JSON structure and audit fields
            JsonNode rootNode = objectMapper.readTree(matchedRecord.value());
            assertThat(rootNode.isObject()).isTrue();
            assertThat(rootNode.get("eventId").asText()).isEqualTo(eventId.toString());
            assertThat(rootNode.get("eventType").asText()).isEqualTo(eventType);
            assertThat(rootNode.get("actorUserId").asLong()).isEqualTo(actorUserId);
            assertThat(rootNode.get("resourceType").asText()).isEqualTo(resourceType);
            assertThat(rootNode.get("resourceId").asText()).isEqualTo(resourceId);
            assertThat(rootNode.get("outcome").asText()).isEqualTo(outcome);
            assertThat(rootNode.get("correlationId").asText()).isEqualTo(correlationId);
            assertThat(rootNode.get("serviceName").asText()).isEqualTo("auth-service");
            assertThat(matchedRecord.value()).doesNotContain("attempted-override");
        }
    }
}
