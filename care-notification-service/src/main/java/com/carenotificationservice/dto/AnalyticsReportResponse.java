package com.carenotificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportResponse {
    private Map<String, Long> eventCounts;
    private Map<String, Object> usageStatistics;
    private Long totalUsers;
    private Long activeUsers;
    private Long totalMatches;
    private Long totalOffers;
    private Long totalSubscriptions;
}
