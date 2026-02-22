package com.carematchservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDTO {
    private UUID id;
    private UUID userId;
    private Integer age;
    private String gender;
    private String region;
    private Double latitude;
    private Double longitude;
    private Integer careLevel;
    private List<String> careType;
    private Map<String, Object> lifestyleAttributes;
    private Map<String, Object> medicalRequirements;
}
