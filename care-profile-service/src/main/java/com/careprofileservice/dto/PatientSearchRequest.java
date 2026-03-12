package com.careprofileservice.dto;

import com.careprofileservice.model.CareServiceTier;
import lombok.Data;

import java.util.List;

@Data
public class PatientSearchRequest {

    /**
     * Filter by care service tier — the most strategically important filter.
     * Providers offering PREMIUM services typically target PREMIUM-tier patients.
     */
    private CareServiceTier careServiceTier;

    /** Filter by Pflegegrad (1–5). */
    private Integer careLevel;

    /** Filter by care type (RESIDENTIAL, AMBULATORY, etc.). */
    private List<String> careType;

    /** Filter by region string (partial match). */
    private String region;

    /** Maximum distance from provider location in kilometres. */
    private Double maxDistanceKm;

    /** Minimum patient age. */
    private Integer minAge;

    /** Maximum patient age. */
    private Integer maxAge;

    // Pagination
    private Integer page;
    private Integer size;
}