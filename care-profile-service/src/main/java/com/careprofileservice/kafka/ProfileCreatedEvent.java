package com.careprofileservice.kafka;

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
    private String eventType = "profile.created";
    private UUID profileId;
    private String profileType; // patient or provider
    private UUID userId;
    private LocalDateTime timestamp;
}
