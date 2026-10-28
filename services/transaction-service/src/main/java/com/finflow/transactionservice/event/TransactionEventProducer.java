package com.finflow.transactionservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final String TOPIC =
            "finflow.transaction.events";

    private final KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate;

    public void publishTransactionCompleted(
            TransactionCompletedEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.getTransactionReference().toString(),
                event
        );
    }
}