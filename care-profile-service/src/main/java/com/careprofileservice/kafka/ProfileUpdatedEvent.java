package com.careprofileservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdatedEvent {
    private String eventType = "profile.updated";
    private UUID profileId;
    private String profileType; // patient or provider
    private UUID userId;
    private Map<String, Object> changes; // What fields changed
    private LocalDateTime timestamp;
}
