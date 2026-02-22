package com.carematchservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
}
