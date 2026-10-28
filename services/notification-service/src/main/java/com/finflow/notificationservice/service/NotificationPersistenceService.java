package com.finflow.notificationservice.service;

import com.finflow.notificationservice.entity.Notification;
import com.finflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void persistNotifications(List<Notification> notifications) {
        notificationRepository.saveAll(notifications);
    }
}
