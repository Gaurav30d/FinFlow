package com.finflow.notificationservice.controller;

import com.finflow.notificationservice.entity.Notification;
import com.finflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/notifications", "/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getMyNotifications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return notificationService.getMyNotifications(userId);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return notificationService.getUnreadCount(userId);
    }
}