package com.careprofileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full provider detail returned by GET /providers/{id}/public.
 * Includes the full profile data plus the facility media list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPublicDetailResponse {

    // ── Core identity ─────────────────────────────────────────────────────────
    private UUID id;
    private String facilityName;
    private String providerType;        // RESIDENTIAL | AMBULATORY
    private String email;               // public contact email (may be masked by provider)

    // ── Location ──────────────────────────────────────────────────────────────
    private String address;
    private String region;
    private Double latitude;
    private Double longitude;

    // ── Residential-specific ─────────────────────────────────────────────────
    private Integer capacity;
    private Integer availableRooms;
    private Map<String, Integer> roomTypes;

    // ── Ambulatory-specific ──────────────────────────────────────────────────
    private Integer serviceRadius;
    private Integer maxDailyPatients;

    // ── Common care attributes ────────────────────────────────────────────────
    private List<String> specializations;
    private Integer staffCount;
    private BigDecimal staffToPatientRatio;
    private Map<String, Object> availability;
    private Map<String, Object> qualityIndicators;
    private Map<String, Object> lifestyleOptions;
    private List<Integer> acceptedCareLevels;

    // ── Facility media ────────────────────────────────────────────────────────
    /** All FACILITY_MEDIA documents (images + videos) uploaded by the provider. */
    private List<DocumentResponse> facilityMedia;

    private boolean isVisible;
}