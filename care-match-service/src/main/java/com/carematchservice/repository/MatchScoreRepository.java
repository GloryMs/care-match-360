package com.carematchservice.repository;

import com.carematchservice.model.MatchScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchScoreRepository extends JpaRepository<MatchScore, UUID> {

    Optional<MatchScore> findByPatientIdAndProviderId(UUID patientId, UUID providerId);

    List<MatchScore> findByPatientIdOrderByScoreDesc(UUID patientId);

    Page<MatchScore> findByPatientIdOrderByScoreDesc(UUID patientId, Pageable pageable);

    List<MatchScore> findByProviderIdOrderByScoreDesc(UUID providerId);

    Page<MatchScore> findByProviderIdOrderByScoreDesc(UUID providerId, Pageable pageable);

    @Query("SELECT m FROM MatchScore m WHERE m.patientId = :patientId AND m.score >= :minScore ORDER BY m.score DESC")
    List<MatchScore> findByPatientIdAndScoreGreaterThanEqual(
            @Param("patientId") UUID patientId,
            @Param("minScore") BigDecimal minScore
    );

    @Query("SELECT m FROM MatchScore m WHERE m.providerId = :providerId AND m.score >= :minScore ORDER BY m.score DESC")
    List<MatchScore> findByProviderIdAndScoreGreaterThanEqual(
            @Param("providerId") UUID providerId,
            @Param("minScore") BigDecimal minScore
    );

    void deleteByPatientId(UUID patientId);

    void deleteByProviderId(UUID providerId);

    long countByPatientId(UUID patientId);
}
