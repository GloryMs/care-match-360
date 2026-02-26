package com.carenotificationservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carenotificationservice.dto.AnalyticsReportResponse;
import com.carenotificationservice.dto.EventLogResponse;
import com.carenotificationservice.dto.UsageMetricResponse;
import com.carenotificationservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
//@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/events/profile/{profileId}")
    //@Operation(summary = "Get event logs for a patient/provider/admin by their profile ID")
    public ResponseEntity<ApiResponse<List<EventLogResponse>>> getEventLogsForProfile(
            @PathVariable UUID profileId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<EventLogResponse> eventLogs = analyticsService.getEventLogsForProfile(profileId, page, size);
        return ResponseEntity.ok(ApiResponse.success(eventLogs));
    }

    @GetMapping("/events/type/{eventType}")
    //@Operation(summary = "Get events by type")
    public ResponseEntity<ApiResponse<List<EventLogResponse>>> getEventsByType(
            @PathVariable String eventType) {

        List<EventLogResponse> eventLogs = analyticsService.getEventsByType(eventType);
        return ResponseEntity.ok(ApiResponse.success(eventLogs));
    }

    @GetMapping("/events/time-range")
    //@Operation(summary = "Get events by time range")
    public ResponseEntity<ApiResponse<List<EventLogResponse>>> getEventsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<EventLogResponse> eventLogs = analyticsService.getEventsByTimeRange(start, end);
        return ResponseEntity.ok(ApiResponse.success(eventLogs));
    }

    @GetMapping("/events/counts")
    //@Operation(summary = "Get event counts by type")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getEventCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        Map<String, Long> eventCounts = analyticsService.getEventCountsByType(start, end);
        return ResponseEntity.ok(ApiResponse.success(eventCounts));
    }

    @GetMapping("/metrics/{metricName}")
    //@Operation(summary = "Get metrics by name")
    public ResponseEntity<ApiResponse<List<UsageMetricResponse>>> getMetricsByName(
            @PathVariable String metricName) {

        List<UsageMetricResponse> metrics = analyticsService.getMetricsByName(metricName);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/metrics/{metricName}/recent")
    //@Operation(summary = "Get recent metrics")
    public ResponseEntity<ApiResponse<List<UsageMetricResponse>>> getRecentMetrics(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "30") int days) {

        List<UsageMetricResponse> metrics = analyticsService.getRecentMetrics(metricName, days);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/report")
    //@Operation(summary = "Generate analytics report")
    public ResponseEntity<ApiResponse<AnalyticsReportResponse>> generateReport() {
        AnalyticsReportResponse report = analyticsService.generateAnalyticsReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
