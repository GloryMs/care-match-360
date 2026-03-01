package com.carematchservice.dto;

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
public class CareRequestResponse {

    private UUID id;
    private UUID patientId;
    private UUID providerId;
    private String status;           // PENDING | ACCEPTED | DECLINED
    private String patientMessage;
    private String declineReason;
    private UUID linkedOfferId;      // populated when provider creates an offer in response
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime respondedAt;

    // Enriched fields (fetched from profile service for display convenience)
    private String providerName;     // facilityName of the provider
    private String providerType;     // RESIDENTIAL | AMBULATORY
    private String providerAddress;
}