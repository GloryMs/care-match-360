package com.carenotificationservice.dto;

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
public class EventLogResponse {
    private UUID id;
    private UUID profileId;
    private String eventType;
    private Map<String, Object> eventData;
    private LocalDateTime timestamp;
}
