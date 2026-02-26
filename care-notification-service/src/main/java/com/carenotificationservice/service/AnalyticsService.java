package com.carenotificationservice.service;

import com.carenotificationservice.dto.AnalyticsReportResponse;
import com.carenotificationservice.dto.EventLogResponse;
import com.carenotificationservice.dto.UsageMetricResponse;
import com.carenotificationservice.model.EventLog;
import com.carenotificationservice.model.UsageMetric;
import com.carenotificationservice.repository.EventLogRepository;
import com.carenotificationservice.repository.UsageMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final EventLogRepository eventLogRepository;
    private final UsageMetricRepository usageMetricRepository;

    @Value("${app.analytics.retention-days}")
    private int retentionDays;

    @Transactional
    public void logEvent(UUID profileId, String eventType, Map<String, Object> eventData) {
        EventLog eventLog = EventLog.builder()
                .profileId(profileId)
                .eventType(eventType)
                .eventData(eventData)
                .timestamp(LocalDateTime.now())
                .build();

        eventLogRepository.save(eventLog);
        log.debug("Event logged: profileId={}, eventType={}", profileId, eventType);
    }

    @Transactional(readOnly = true)
    public List<EventLogResponse> getEventLogsForProfile(UUID profileId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        Page<EventLog> eventLogsPage = eventLogRepository.findByProfileIdOrderByTimestampDesc(profileId, pageable);

        ModelMapper modelMapper = new ModelMapper();
        return eventLogsPage.getContent().stream()
                .map(eventLog -> modelMapper.map(eventLog, EventLogResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventLogResponse> getEventsByType(String eventType) {
        List<EventLog> eventLogs = eventLogRepository.findByEventType(eventType);

        ModelMapper modelMapper = new ModelMapper();
        return eventLogs.stream()
                .map(eventLog -> modelMapper.map(eventLog, EventLogResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventLogResponse> getEventsByTimeRange(LocalDateTime start, LocalDateTime end) {
        List<EventLog> eventLogs = eventLogRepository.findByTimestampBetween(start, end);

        ModelMapper modelMapper = new ModelMapper();
        return eventLogs.stream()
                .map(eventLog ->  modelMapper.map(eventLog, EventLogResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getEventCountsByType(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = eventLogRepository.countEventsByType(start, end);

        Map<String, Long> eventCounts = new HashMap<>();
        for (Object[] result : results) {
            String eventType = (String) result[0];
            Long count = (Long) result[1];
            eventCounts.put(eventType, count);
        }

        return eventCounts;
    }

    @Transactional
    public void recordMetric(String metricName, BigDecimal metricValue,
                             UsageMetric.AggregationPeriod period) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = calculatePeriodStart(now, period);
        LocalDateTime periodEnd = calculatePeriodEnd(periodStart, period);

        // Check if metric already exists for this period
        Optional<UsageMetric> existingMetric = usageMetricRepository
                .findByMetricNameAndPeriodStartAndAggregationPeriod(metricName, periodStart, period);

        if (existingMetric.isPresent()) {
            // Update existing metric
            UsageMetric metric = existingMetric.get();
            metric.setMetricValue(metricValue);
            metric.setRecordedAt(LocalDateTime.now());
            usageMetricRepository.save(metric);
        } else {
            // Create new metric
            UsageMetric metric = UsageMetric.builder()
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .aggregationPeriod(period)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .build();

            usageMetricRepository.save(metric);
        }

        log.debug("Metric recorded: name={}, value={}, period={}", metricName, metricValue, period);
    }

    @Transactional(readOnly = true)
    public List<UsageMetricResponse> getMetricsByName(String metricName) {
        List<UsageMetric> metrics = usageMetricRepository.findByMetricNameOrderByPeriodStartDesc(metricName);

        ModelMapper mapper = new ModelMapper();
        return metrics.stream()
                .map(usageMetric -> mapper.map(usageMetric, UsageMetricResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UsageMetricResponse> getRecentMetrics(String metricName, int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<UsageMetric> metrics = usageMetricRepository.findRecentMetrics(metricName, start);

        ModelMapper mapper = new ModelMapper();
        return metrics.stream()
                .map(metric -> mapper.map(metric, UsageMetricResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnalyticsReportResponse generateAnalyticsReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // Get event counts
        Map<String, Long> eventCounts = getEventCountsByType(startOfDay, endOfDay);

        // Get usage statistics
        Map<String, Object> usageStatistics = new HashMap<>();

        List<UsageMetric> todayMetrics = usageMetricRepository.findByAggregationPeriod(
                UsageMetric.AggregationPeriod.DAILY
        );

        for (UsageMetric metric : todayMetrics) {
            if (metric.getPeriodStart().isAfter(startOfDay.minusDays(1))) {
                usageStatistics.put(metric.getMetricName(), metric.getMetricValue());
            }
        }

        return AnalyticsReportResponse.builder()
                .eventCounts(eventCounts)
                .usageStatistics(usageStatistics)
                .totalUsers(getLongFromMap(usageStatistics, "total_users"))
                .activeUsers(getLongFromMap(usageStatistics, "active_users"))
                .totalMatches(getLongFromMap(usageStatistics, "total_matches"))
                .totalOffers(getLongFromMap(usageStatistics, "total_offers"))
                .totalSubscriptions(getLongFromMap(usageStatistics, "active_subscriptions"))
                .build();
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void aggregateDailyMetrics() {
        log.info("Running daily metrics aggregation");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime startOfYesterday = yesterday.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfYesterday = startOfYesterday.plusDays(1);

        // Aggregate event counts
        Map<String, Long> eventCounts = getEventCountsByType(startOfYesterday, endOfYesterday);

        for (Map.Entry<String, Long> entry : eventCounts.entrySet()) {
            recordMetric(
                    "event_count_" + entry.getKey(),
                    BigDecimal.valueOf(entry.getValue()),
                    UsageMetric.AggregationPeriod.DAILY
            );
        }

        log.info("Daily metrics aggregation completed");
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // Run daily at 3 AM
    public void cleanupOldData() {
        log.info("Running data cleanup job");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        // Delete old event logs
        eventLogRepository.deleteByTimestampBefore(cutoffDate);

        // Delete old metrics
        usageMetricRepository.deleteByRecordedAtBefore(cutoffDate);

        log.info("Data cleanup completed: deleted data older than {} days", retentionDays);
    }

    private LocalDateTime calculatePeriodStart(LocalDateTime timestamp, UsageMetric.AggregationPeriod period) {
        return switch (period) {
            case HOURLY -> timestamp.truncatedTo(ChronoUnit.HOURS);
            case DAILY -> timestamp.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> timestamp.truncatedTo(ChronoUnit.DAYS)
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case MONTHLY -> timestamp.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        };
    }

    private LocalDateTime calculatePeriodEnd(LocalDateTime periodStart, UsageMetric.AggregationPeriod period) {
        return switch (period) {
            case HOURLY -> periodStart.plusHours(1);
            case DAILY -> periodStart.plusDays(1);
            case WEEKLY -> periodStart.plusWeeks(1);
            case MONTHLY -> periodStart.plusMonths(1);
        };
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}