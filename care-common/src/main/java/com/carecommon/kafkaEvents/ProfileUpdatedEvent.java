package com.carecommon.kafkaEvents;

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
    private String eventType;
    private UUID profileId;
    private String profileType;
    private UUID userId;
    private Map<String, Object> changes;
    private LocalDateTime timestamp;
}
