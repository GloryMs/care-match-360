package com.carenotificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetricResponse {
    private UUID id;
    private String metricName;
    private BigDecimal metricValue;
    private String aggregationPeriod;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime recordedAt;
}
