package com.careprofileservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
}