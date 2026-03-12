package com.careprofileservice.dto;

import com.careprofileservice.model.CareServiceTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private Integer age;
    private String gender;
    private String region;
    private Double latitude;
    private Double longitude;
    private Integer careLevel;
    private List<String> careType;
    private Map<String, Object> lifestyleAttributes;
    private Map<String, Object> medicalRequirements;
    private Map<String, Boolean> dataVisibility;
    private Boolean consentGiven;
    /** Patient's affordable / preferred care service tier — always public. */
    private CareServiceTier careServiceTier;
    /** Always true — profile is visible to all subscribed providers. */
    private Boolean profilePublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
