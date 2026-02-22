package com.careprofileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProviderProfileRequest {

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
    private Boolean isVisible;
}
