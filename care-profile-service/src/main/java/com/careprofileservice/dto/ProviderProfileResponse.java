package com.careprofileservice.dto;


import com.careprofileservice.model.ProviderServiceTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderProfileResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private String facilityName;
    private String providerType;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer capacity;
    private Integer availableRooms;
    private Map<String, Integer> roomTypes;
    private Integer serviceRadius;
    private Integer maxDailyPatients;
    private List<String> specializations;
    private Integer staffCount;
    private BigDecimal staffToPatientRatio;
    private Map<String, Object> availability;
    private Map<String, Object> qualityIndicators;
    private String introVideoUrl;
    private List<String> images;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Service tiers supported by this provider. */
    private Set<ProviderServiceTier> offeredServiceTiers;

    /** Human-readable list of VIP / luxury services. */
    private List<String> premiumServices;

    // Profile enrichment fields
    private String description;
    private String aboutText;
    private String phoneNumber;
    private String website;
    private Map<String, String> socialLinks;
    private Integer yearEstablished;
    private Double rating;
    private Map<String, String> operatingHours;
    private List<String> servicesOffered;
    private List<String> languagesSupported;
    private List<String> insuranceAccepted;
}