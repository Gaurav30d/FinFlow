package com.finflow.notificationservice.consumer;

import com.finflow.notificationservice.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionEventConsumer {

    @KafkaListener(
            topics = "finflow.transaction.events",
            groupId = "finflow-notification-group"
    )
    public void consumeTransactionCompleted(
            TransactionCompletedEvent event) {

        if (!"TRANSACTION_COMPLETED".equals(event.getEventType())) {
            log.warn(
                    "Ignoring unsupported event type: {}",
                    event.getEventType()
            );
            return;
        }

        log.info(
                "NOTIFICATION: Transaction completed | Reference: {} | " +
                        "Amount: {} {} | Sender: {} | Receiver: {}",
                event.getTransactionReference(),
                event.getAmount(),
                event.getCurrency(),
                event.getSenderUserId(),
                event.getReceiverUserId()
        );

        log.info(
                "Notification for sender user {}: Your transfer of {} {} was successful.",
                event.getSenderUserId(),
                event.getAmount(),
                event.getCurrency()
        );

        log.info(
                "Notification for receiver user {}: You received {} {}.",
                event.getReceiverUserId(),
                event.getAmount(),
                event.getCurrency()
        );
    }
}