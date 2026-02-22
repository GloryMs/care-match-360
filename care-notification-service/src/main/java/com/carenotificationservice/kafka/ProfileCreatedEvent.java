package com.carenotificationservice.kafka;

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
public class ProfileCreatedEvent {
    private String eventType;
    private UUID profileId;
    private String profileType;
    private UUID userId;
    private LocalDateTime timestamp;
}