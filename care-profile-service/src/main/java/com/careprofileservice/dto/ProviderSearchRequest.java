package com.careprofileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSearchRequest {

    private String providerType; // residential or ambulatory
    private String region;
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;
    private Integer careLevel;
    private List<String> specializations;
    private LocalDate availabilityDate;
    private String roomType;
    private Integer minCapacity;

    // Pagination
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    // Sorting
    private String sortBy; // distance, matchScore, name

    @Builder.Default
    private String sortDirection = "asc";
}