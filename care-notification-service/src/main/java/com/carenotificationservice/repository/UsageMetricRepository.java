package com.carenotificationservice.repository;

import com.carenotificationservice.model.UsageMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageMetricRepository extends JpaRepository<UsageMetric, UUID> {

    List<UsageMetric> findByMetricNameOrderByPeriodStartDesc(String metricName);

    List<UsageMetric> findByAggregationPeriod(UsageMetric.AggregationPeriod period);

    @Query("SELECT m FROM UsageMetric m WHERE m.metricName = :metricName AND m.periodStart >= :start ORDER BY m.periodStart DESC")
    List<UsageMetric> findRecentMetrics(@Param("metricName") String metricName, @Param("start") LocalDateTime start);

    Optional<UsageMetric> findByMetricNameAndPeriodStartAndAggregationPeriod(
            String metricName,
            LocalDateTime periodStart,
            UsageMetric.AggregationPeriod aggregationPeriod
    );

    void deleteByRecordedAtBefore(LocalDateTime dateTime);
}
