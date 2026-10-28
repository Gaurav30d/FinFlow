package com.finflow.notificationservice.controller;

import com.finflow.notificationservice.entity.Notification;
import com.finflow.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private final Long userId = 101L;
    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    @DisplayName("GET /api/notifications returns user-scoped notifications list")
    void getMyNotifications_ApiPrefix_ReturnsUserNotifications() throws Exception {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipientUserId(userId);
        notification.setType("TRANSFER_SENT");
        notification.setMessage("Your transfer of 100.00 INR was successful.");
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);

        when(notificationService.getMyNotifications(userId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientUserId").value(userId))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_SENT"))
                .andExpect(jsonPath("$[0].message").value("Your transfer of 100.00 INR was successful."))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationService).getMyNotifications(userId);
    }

    @Test
    @DisplayName("GET /notifications also resolves successfully")
    void getMyNotifications_DirectRoute_ReturnsUserNotifications() throws Exception {
        when(notificationService.getMyNotifications(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notifications").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(notificationService).getMyNotifications(userId);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks notification as read and returns 204")
    void markAsRead_Success_Returns204() throws Exception {
        UUID notificationId = UUID.randomUUID();

        doNothing().when(notificationService).markAsRead(notificationId, userId);

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read").principal(authentication))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(notificationId, userId);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read returns 404 when notification not found or not owned")
    void markAsRead_NotFound_Returns404() throws Exception {
        UUID notificationId = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"))
                .when(notificationService).markAsRead(notificationId, userId);

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read").principal(authentication))
                .andExpect(status().isNotFound());

        verify(notificationService).markAsRead(notificationId, userId);
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns unread count for user")
    void getUnreadCount_ReturnsCount() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

        verify(notificationService).getUnreadCount(userId);
    }
}
