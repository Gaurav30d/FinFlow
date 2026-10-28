package com.finflow.notificationservice.service;

import com.finflow.notificationservice.entity.Notification;
import com.finflow.notificationservice.event.TransactionCompletedEvent;
import com.finflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPersistenceService persistenceService;

    public void processTransactionCompleted(TransactionCompletedEvent event) {

        UUID eventId = event.getEventId() != null ? event.getEventId() : event.getTransactionReference();
        Long senderUserId = event.getSenderUserId();
        Long receiverUserId = event.getReceiverUserId();

        // Handle self-transaction
        if (senderUserId.equals(receiverUserId)) {
            boolean exists = notificationRepository.existsByEventIdAndRecipientUserId(eventId, senderUserId);
            if (exists) {
                log.info("Event {} for user {} already processed. Skipping duplicate.", eventId, senderUserId);
                return;
            }

            Notification notification = createNotification(
                    event,
                    eventId,
                    senderUserId,
                    "Your transaction of " + event.getAmount() + " " + event.getCurrency() + " was completed.",
                    "TRANSACTION_COMPLETED"
            );

            try {
                persistenceService.persistNotifications(List.of(notification));
                log.info("Persisted self-transaction notification for user {}", senderUserId);
            } catch (DataIntegrityViolationException ex) {
                if (notificationRepository.existsByEventIdAndRecipientUserId(eventId, senderUserId)) {
                    log.info("Concurrent duplicate event {} for user {} ignored.", eventId, senderUserId);
                    return;
                }
                throw ex;
            }
            return;
        }

        // Standard peer-to-peer transfer
        boolean senderExists = notificationRepository.existsByEventIdAndRecipientUserId(eventId, senderUserId);
        boolean receiverExists = notificationRepository.existsByEventIdAndRecipientUserId(eventId, receiverUserId);

        if (senderExists && receiverExists) {
            log.info("Event {} already processed for both sender and receiver. Skipping duplicate.", eventId);
            return;
        }

        List<Notification> toSave = new ArrayList<>();

        if (!senderExists) {
            String senderMsg = "Your transfer of " + event.getAmount() + " " + event.getCurrency()
                    + " to account " + maskAccountNumber(event.getReceiverAccountNumber()) + " was successful.";
            toSave.add(createNotification(event, eventId, senderUserId, senderMsg, "TRANSFER_SENT"));
        }

        if (!receiverExists) {
            String receiverMsg = "You received " + event.getAmount() + " " + event.getCurrency()
                    + " from account " + maskAccountNumber(event.getSenderAccountNumber()) + ".";
            toSave.add(createNotification(event, eventId, receiverUserId, receiverMsg, "TRANSFER_RECEIVED"));
        }

        try {
            persistenceService.persistNotifications(toSave);
            log.info("Persisted {} notification(s) for event {} and transaction {}",
                    toSave.size(), eventId, event.getTransactionReference());

        } catch (DataIntegrityViolationException ex) {
            boolean sExists = notificationRepository.existsByEventIdAndRecipientUserId(eventId, senderUserId);
            boolean rExists = notificationRepository.existsByEventIdAndRecipientUserId(eventId, receiverUserId);

            if (sExists || rExists) {
                log.info("Concurrent duplicate event {} safely handled.", eventId);
                return;
            }

            throw ex;
        }
    }

    private Notification createNotification(
            TransactionCompletedEvent event,
            UUID eventId,
            Long recipientUserId,
            String message,
            String type) {

        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setTransactionReference(event.getTransactionReference());
        notification.setRecipientUserId(recipientUserId);
        notification.setMessage(message);
        notification.setType(type);

        return notification;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber != null ? accountNumber : "";
        }
        return "••••" + accountNumber.substring(accountNumber.length() - 4);
    }

    @Transactional
    public void markAsRead(UUID notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByRecipientUserIdAndReadFalse(userId);
    }
}