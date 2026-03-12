package com.careprofileservice.dto;

import com.careprofileservice.model.CareServiceTier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientProfileRequest {

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Region is required")
    private String region;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Care level is required")
    @Min(value = 1, message = "Care level must be between 1 and 5")
    @Max(value = 5, message = "Care level must be between 1 and 5")
    private Integer careLevel;

    @NotEmpty(message = "Care type is required")
    private List<String> careType;

    private Map<String, Object> lifestyleAttributes;

    private Map<String, Object> medicalRequirements;

    private Map<String, Boolean> dataVisibility;

    @NotNull(message = "Consent must be provided")
    private Boolean consentGiven;

    /**
     * Patient's affordable / preferred care service tier.
     * Defaults to STANDARD if not supplied.
     *
     * STANDARD  → statutory GKV care only
     * COMFORT   → mid-range with selected premium amenities
     * PREMIUM   → full VIP / private-pay care
     */
    private CareServiceTier careServiceTier = CareServiceTier.STANDARD;
}
