package com.finflow.notificationservice.repository;

import com.finflow.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventIdAndRecipientUserId(UUID eventId, Long recipientUserId);

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, Long recipientUserId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}