package com.carematchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSummaryDTO {
    private String name;
    private String type;
    private List<String> specializations;
    private Double distanceKm;
    private Boolean available;
}
