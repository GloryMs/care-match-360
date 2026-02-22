package com.carematchservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreResponse {
    private UUID id;
    private UUID patientId;
    private UUID providerId;
    private BigDecimal score;
    private Map<String, Object> explanation;
    private Map<String, Object> scoreBreakdown;
    private LocalDateTime calculatedAt;

    // Optional: Include provider details for convenience
    private String providerName;
    private String providerType;
    private String providerAddress;
}
