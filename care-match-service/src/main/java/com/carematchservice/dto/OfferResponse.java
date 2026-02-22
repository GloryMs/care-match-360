package com.carematchservice.dto;


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
public class OfferResponse {
    private UUID id;
    private UUID patientId;
    private UUID providerId;
    private UUID matchId;
    private String status;
    private String message;
    private Map<String, Object> availabilityDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    // Optional: Include provider/patient details
    private String providerName;
    private String patientName;
    private Double matchScore;
}