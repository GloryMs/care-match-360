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
public class SubscriptionExpiredEvent {
    private String eventType = "subscription.expired";
    private UUID subscriptionId;
    private UUID providerId;
    private LocalDateTime timestamp;
}
