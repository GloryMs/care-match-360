package com.carematchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusDTO {
    private UUID providerId;
    private boolean isActive;    // true when status is ACTIVE or TRIALING
    private String status;       // ACTIVE | TRIALING | PAUSED | CANCELLED | PAST_DUE
}