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
    private LocalDateTime calculatedAt;

    // Optional: Include provider details for convenience
    private String providerName;
    private String providerType;
    private String providerAddress;

    /**
     * Detailed breakdown per scoring dimension.
     * Keys: careLevel, distance, specialization, servicetier, lifestyle, social, quality
     * Values: contribution score 0–100
     */
    private Map<String, Double> scoreBreakdown;

    /** Tier offered by this provider — useful for display. */
    private java.util.Set<String> providerServiceTiers;
    private java.util.List<String> providerPremiumServices;
}
