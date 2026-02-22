package com.carenotificationservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCalculatedEvent {
    private String eventType;
    private UUID matchId;
    private UUID patientId;
    private UUID providerId;
    private BigDecimal score;
    private LocalDateTime timestamp;
}
