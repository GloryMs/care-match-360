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
public class CareRequestDeclinedEvent {
    private String eventType = "care-request.declined";
    private UUID requestId;
    private UUID patientId;       // patient profile UUID
    private UUID providerId;      // provider profile UUID
    private String providerName;
    private String declineReason;
    private LocalDateTime timestamp;
}