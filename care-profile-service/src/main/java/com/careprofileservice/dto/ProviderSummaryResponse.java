package com.careprofileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight DTO returned by the public provider directory (GET /providers).
 * Intentionally omits sensitive / large fields to keep listing payloads small.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSummaryResponse {

    private UUID id;
    private String facilityName;
    private String providerType;        // RESIDENTIAL | AMBULATORY
    private String address;
    private String region;
    private Double latitude;
    private Double longitude;

    /** Min care-level supported (derived from acceptedCareLevels if present). */
    private Integer minCareLevel;
    private Integer maxCareLevel;

    private List<String> specializations;

    /** URL of the first FACILITY_MEDIA image, if any (thumbnail). */
    private String primaryImageUrl;

    /** Total number of facility media attachments uploaded by this provider. */
    private int mediaCount;

    private boolean isVisible;
}