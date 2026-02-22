package com.carenotificationservice.repository;

import com.carenotificationservice.model.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, UUID> {

    List<EventLog> findByUserIdOrderByTimestampDesc(UUID userId);

    Page<EventLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    List<EventLog> findByEventType(String eventType);

    @Query("SELECT e FROM EventLog e WHERE e.timestamp BETWEEN :start AND :end")
    List<EventLog> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT e.eventType, COUNT(e) FROM EventLog e WHERE e.timestamp BETWEEN :start AND :end GROUP BY e.eventType")
    List<Object[]> countEventsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteByTimestampBefore(LocalDateTime dateTime);
}
