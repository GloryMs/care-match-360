package com.carebillingservice.kafka.events;

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
public class SubscriptionCreatedEvent {
    private String eventType = "subscription.created";
    private UUID subscriptionId;
    private UUID providerId;
    private String tier;
    private LocalDateTime timestamp;
}
