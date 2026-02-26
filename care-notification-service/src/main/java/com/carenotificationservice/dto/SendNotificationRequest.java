package com.carenotificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    // Required for EMAIL type notifications
    private String recipientEmail;

    @NotBlank(message = "Notification type is required")
    private String type; // EMAIL, IN_APP, PUSH

    @NotBlank(message = "Channel is required")
    private String channel; // new_match, offer_received, etc.

    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private Map<String, Object> templateData; // For template-based notifications
}
