package com.careprofileservice.repository;


import com.careprofileservice.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(UUID userId);

    List<SearchHistory> findBySearchedAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteBySearchedAtBefore(LocalDateTime dateTime);
}
