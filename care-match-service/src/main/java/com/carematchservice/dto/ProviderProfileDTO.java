package com.carematchservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderProfileDTO {
    private UUID id;
    private UUID userId;
    private String facilityName;
    private String providerType;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer capacity;
    private Integer availableRooms;
    private List<String> specializations;
    private Integer staffCount;
    private BigDecimal staffToPatientRatio;
    private Map<String, Object> availability;
    /**
     * Service tiers offered by this provider.
     * Values: "STANDARD" | "COMFORT" | "PREMIUM"
     * A provider may offer multiple tiers.
     */
    private Set<String> offeredServiceTiers;
    /** VIP / luxury services list — informational, used in explanations. */
    private List<String> premiumServices;
    // ── Demographics ──────────────────────────────────────────────────────────
    private Integer age;
    private String  gender;
    private String  region;
    // ── Care requirements ─────────────────────────────────────────────────────
    /** Pflegegrad 1–5. */
    private Integer      careLevel;
    /** E.g. ["RESIDENTIAL", "DEMENTIA_CARE"]. */
    private List<String> careType;
    // ── Lifestyle & medical ───────────────────────────────────────────────────
    /**
     * Free-form lifestyle attribute map.
     * Keys consumed by calculateLifestyleScore():
     *   "petsAllowed"           → "true" | "false"
     *   "smokingAllowed"        → "true" | "false"
     *   "dietaryRequirements"   → free text, e.g. "vegetarian"
     * Keys consumed by calculateSocialScore():
     *   "socialInteractionLevel" → "low" | "medium" | "high"
     *   "groupSizePreference"    → free text
     */
    private Map<String, Object> lifestyleAttributes;
    /**
     * Free-form medical requirements map.
     * Keys consumed by calculateSpecializationScore():
     *   boolean flags e.g. "dementiaSupport": "true", "palliativeCare": "true"
     *   "conditions": comma-separated string, e.g. "dementia,diabetes"
     */
    private Map<String, Object> medicalRequirements;
    // ── Care Service Tier ─────────────────────────────────────────────────────
    /**
     * Patient's affordable / preferred care service tier.
     * Values: "STANDARD" | "COMFORT" | "PREMIUM"
     * Used by calculateTierScore() in MatchingAlgorithmService (15% weight).
     * Defaults to "STANDARD" when null (safe fallback, neutral scoring).
     * Set by the patient on their profile form; always public to providers.
     */
    private String careServiceTier;
    // ── previously missing — present on entity & mapped in ProviderProfileService ──
    private Map<String, Object> qualityIndicators;  // e.g. {"averageRating": 4.5, "certifications": "ISO_9001"}
    private Map<String, Object> lifestyleOptions;   // e.g. {"petsAllowed": true, "smokingAllowed": false}
    private List<Integer> acceptedCareLevels;       // e.g. [2, 3, 4]


}
