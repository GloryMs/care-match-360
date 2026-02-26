package com.carenotificationservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carenotificationservice.dto.NotificationResponse;
import com.carenotificationservice.dto.SendNotificationRequest;
import com.carenotificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
//@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    //@Operation(summary = "Send notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse notification = notificationService.sendNotification(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(notification, "Notification sent successfully"));
    }

    @GetMapping("/{recipientId}")
    //@Operation(summary = "Get notifications for a patient/provider/admin by their profile ID")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsForRecipient(
            @PathVariable UUID recipientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<NotificationResponse> notifications = notificationService.getNotificationsForRecipient(
                recipientId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{recipientId}/unread")
    //@Operation(summary = "Get unread notifications for a patient/provider/admin")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @PathVariable UUID recipientId) {

        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(recipientId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{recipientId}/unread/count")
    //@Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable UUID recipientId) {
        Long count = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{notificationId}/read")
    //@Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PutMapping("/{recipientId}/read-all")
    //@Operation(summary = "Mark all notifications as read for a recipient")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable UUID recipientId) {
        notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
