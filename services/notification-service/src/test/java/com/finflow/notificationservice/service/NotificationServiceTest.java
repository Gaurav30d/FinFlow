package com.finflow.notificationservice.service;

import com.finflow.notificationservice.entity.Notification;
import com.finflow.notificationservice.event.TransactionCompletedEvent;
import com.finflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPersistenceService persistenceService;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationListCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, persistenceService);
    }

    private TransactionCompletedEvent createCompletedEvent(UUID eventId, UUID txRef, Long senderId, Long receiverId) {
        return TransactionCompletedEvent.builder()
                .eventId(eventId)
                .transactionReference(txRef)
                .eventType("TRANSACTION_COMPLETED")
                .senderUserId(senderId)
                .receiverUserId(receiverId)
                .senderAccountNumber("ACC10001")
                .receiverAccountNumber("ACC20002")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .transactionType("TRANSFER")
                .occurredAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("P2P transfer creates notifications for both sender and receiver with masked account numbers")
    void processTransactionCompleted_P2PTransfer_SavesSenderAndReceiver() {
        UUID eventId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        TransactionCompletedEvent event = createCompletedEvent(eventId, txRef, 101L, 202L);

        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 101L)).thenReturn(false);
        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 202L)).thenReturn(false);

        notificationService.processTransactionCompleted(event);

        verify(persistenceService).persistNotifications(notificationListCaptor.capture());
        List<Notification> saved = notificationListCaptor.getValue();
        assertThat(saved).hasSize(2);

        Notification senderNotif = saved.stream()
                .filter(n -> n.getRecipientUserId().equals(101L))
                .findFirst().orElseThrow();
        assertThat(senderNotif.getType()).isEqualTo("TRANSFER_SENT");
        assertThat(senderNotif.getEventId()).isEqualTo(eventId);
        assertThat(senderNotif.getTransactionReference()).isEqualTo(txRef);
        assertThat(senderNotif.getMessage()).contains("500.00 INR");
        assertThat(senderNotif.getMessage()).contains("••••0002");

        Notification receiverNotif = saved.stream()
                .filter(n -> n.getRecipientUserId().equals(202L))
                .findFirst().orElseThrow();
        assertThat(receiverNotif.getType()).isEqualTo("TRANSFER_RECEIVED");
        assertThat(receiverNotif.getEventId()).isEqualTo(eventId);
        assertThat(receiverNotif.getTransactionReference()).isEqualTo(txRef);
        assertThat(receiverNotif.getMessage()).contains("500.00 INR");
        assertThat(receiverNotif.getMessage()).contains("••••0001");
    }

    @Test
    @DisplayName("Self transaction creates a single notification for the user")
    void processTransactionCompleted_SelfTransaction_SavesSingleNotification() {
        UUID eventId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        TransactionCompletedEvent event = createCompletedEvent(eventId, txRef, 101L, 101L);

        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 101L)).thenReturn(false);

        notificationService.processTransactionCompleted(event);

        verify(persistenceService).persistNotifications(notificationListCaptor.capture());
        List<Notification> saved = notificationListCaptor.getValue();
        assertThat(saved).hasSize(1);

        Notification notif = saved.get(0);
        assertThat(notif.getRecipientUserId()).isEqualTo(101L);
        assertThat(notif.getType()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(notif.getMessage()).contains("Your transaction of 500.00 INR was completed.");
    }

    @Test
    @DisplayName("Duplicate event where both notifications already exist skips persistence entirely")
    void processTransactionCompleted_BothExist_SkipsPersistence() {
        UUID eventId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        TransactionCompletedEvent event = createCompletedEvent(eventId, txRef, 101L, 202L);

        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 101L)).thenReturn(true);
        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 202L)).thenReturn(true);

        notificationService.processTransactionCompleted(event);

        verify(persistenceService, never()).persistNotifications(any());
    }

    @Test
    @DisplayName("Partial duplicate persists only missing party notification")
    void processTransactionCompleted_SenderExistsReceiverMissing_PersistsOnlyReceiver() {
        UUID eventId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        TransactionCompletedEvent event = createCompletedEvent(eventId, txRef, 101L, 202L);

        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 101L)).thenReturn(true);
        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 202L)).thenReturn(false);

        notificationService.processTransactionCompleted(event);

        verify(persistenceService).persistNotifications(notificationListCaptor.capture());
        List<Notification> saved = notificationListCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getRecipientUserId()).isEqualTo(202L);
        assertThat(saved.get(0).getType()).isEqualTo("TRANSFER_RECEIVED");
    }

    @Test
    @DisplayName("Handles DataIntegrityViolationException gracefully when duplicate exists")
    void processTransactionCompleted_DataIntegrityViolation_HandledGracefully() {
        UUID eventId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        TransactionCompletedEvent event = createCompletedEvent(eventId, txRef, 101L, 202L);

        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 101L))
                .thenReturn(false)
                .thenReturn(true);
        when(notificationRepository.existsByEventIdAndRecipientUserId(eventId, 202L))
                .thenReturn(false);

        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(persistenceService).persistNotifications(any());

        assertThatCode(() -> notificationService.processTransactionCompleted(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getMyNotifications retrieves only the notifications of the requesting user sorted descending")
    void getMyNotifications_ReturnsUserNotifications() {
        Long userId = 101L;
        Notification n1 = new Notification();
        n1.setRecipientUserId(userId);
        n1.setMessage("Message 1");

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(n1));

        List<Notification> results = notificationService.getMyNotifications(userId);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRecipientUserId()).isEqualTo(userId);
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("markAsRead updates notification read status when owned by user")
    void markAsRead_Success() {
        UUID notifId = UUID.randomUUID();
        Long userId = 101L;
        Notification n = new Notification();
        n.setId(notifId);
        n.setRecipientUserId(userId);
        n.setRead(false);

        when(notificationRepository.findByIdAndRecipientUserId(notifId, userId))
                .thenReturn(Optional.of(n));

        notificationService.markAsRead(notifId, userId);

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("markAsRead throws NOT_FOUND when notification does not belong to user")
    void markAsRead_NotFound_ThrowsResponseStatusException() {
        UUID notifId = UUID.randomUUID();
        Long userId = 101L;

        when(notificationRepository.findByIdAndRecipientUserId(notifId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notifId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUnreadCount returns correct unread count for user")
    void getUnreadCount_ReturnsCount() {
        Long userId = 101L;
        when(notificationRepository.countByRecipientUserIdAndReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);
        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByRecipientUserIdAndReadFalse(userId);
    }
}
