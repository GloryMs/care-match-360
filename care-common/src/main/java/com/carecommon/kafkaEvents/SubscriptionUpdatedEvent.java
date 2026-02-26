package com.carecommon.kafkaEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionUpdatedEvent {
    private String eventType = "subscription.updated";
    private UUID subscriptionId;
    private UUID providerId;
    private String oldTier;
    private String newTier;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime timestamp;
}