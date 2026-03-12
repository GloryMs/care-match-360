package com.careprofileservice.dto;

import com.careprofileservice.model.ProviderServiceTier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProviderProfileRequest {

    @NotBlank(message = "Facility name is required")
    private String facilityName;

    @NotBlank(message = "Provider type is required")
    private String providerType; // residential or ambulatory

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Address is required")
    private String address;

    // Residential specific
    private Integer capacity;
    private Integer availableRooms;
    private Map<String, Integer> roomTypes;

    // Ambulatory specific
    private Integer serviceRadius;
    private Integer maxDailyPatients;

    // Common
    @NotEmpty(message = "At least one specialization is required")
    private List<String> specializations;

    private Integer staffCount;
    private BigDecimal staffToPatientRatio;
    private Map<String, Object> availability;
    private Map<String, Object> qualityIndicators;

    // Optional profile enrichment fields
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

    // ── NEW ───────────────────────────────────────────────────────────────────
    /**
     * Which care service tiers this facility supports.
     * At least one tier must be specified; defaults to [STANDARD].
     */
    private Set<ProviderServiceTier> offeredServiceTiers;

    /**
     * VIP / luxury services available (shown in public directory).
     * Only meaningful when offeredServiceTiers includes COMFORT or PREMIUM.
     * Examples: "Private chef", "Luxury en-suite", "On-site spa"
     */
    private List<String> premiumServices;
    //
}
