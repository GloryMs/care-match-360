package com.carematchservice.service;


import com.carecommon.dto.ApiResponse;
import com.carecommon.exception.ResourceNotFoundException;
import com.carematchservice.dto.MatchScoreResponse;
import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import com.carematchservice.feign.ProfileServiceClient;
import com.carecommon.kafkaEvents.MatchCalculatedEvent;
import com.carematchservice.kafka.MatchingEventProducer;
import com.carematchservice.mapper.MatchScoreMapper;
import com.carematchservice.model.MatchNotification;
import com.carematchservice.model.MatchScore;
import com.carematchservice.repository.MatchNotificationRepository;
import com.carematchservice.repository.MatchScoreRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core matching orchestration service for CareMatch360.
 *
 * Scoring is fully delegated to {@link MatchingAlgorithmService}.
 * This service is responsible for:
 *   - Fetching profiles via Feign (ProfileServiceClient)
 *   - Persisting / updating MatchScore records
 *   - Publishing Kafka events via MatchingEventProducer
 *   - Sending threshold notifications (FR-MATCH-02: score ≥ threshold, default 70)
 *
 * FR-MATCH-01: Both calculateMatchesForPatient and calculateMatchesForProvider
 *              are triggered by Kafka profile.created / profile.updated events
 *              (see MatchingEventConsumer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final MatchScoreRepository           matchScoreRepository;
    private final MatchNotificationRepository    matchNotificationRepository;
    private final ProfileServiceClient           profileServiceClient;
    private final MatchingAlgorithmService       matchingAlgorithmService;
    private final MatchingEventProducer          matchingEventProducer;
    private final MatchScoreMapper               matchScoreMapper;

    @Value("${app.matching.threshold}")
    private int matchingThreshold;

    // ═══════════════════════════════════════════════════════════════════
    //  EXISTING METHOD – calculateMatch (single pair)
    //  Kept intact; only minor additions to align with the two new methods
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public MatchScoreResponse calculateMatch(UUID patientId, UUID providerId) {
        log.info("Calculating match: patientId={}, providerId={}", patientId, providerId);

        PatientProfileDTO  patient  = fetchPatientProfile(patientId);
        ProviderProfileDTO provider = fetchProviderProfile(providerId);

        BigDecimal             score       = matchingAlgorithmService.calculateMatchScore(patient, provider);
        Map<String, Object>    explanation = matchingAlgorithmService.generateExplanation(patient, provider, score);
        Map<String, Object>    breakdown   = matchingAlgorithmService.getScoreBreakdown(patient, provider);

        MatchScore matchScore = matchScoreRepository
                .findByPatientIdAndProviderId(patientId, providerId)
                .orElse(MatchScore.builder()
                        .patientId(patientId)
                        .providerId(providerId)
                        .build());

        matchScore.setScore(score);
        matchScore.setExplanation(explanation);
        matchScore.setScoreBreakdown(breakdown);
        matchScore.setCalculatedAt(LocalDateTime.now());

        matchScore = matchScoreRepository.save(matchScore);
        log.info("Match score saved: matchId={}, score={}", matchScore.getId(), score);

        if (score.doubleValue() >= matchingThreshold) {
            publishMatchCalculatedEvent(matchScore);
        }

        MatchScoreResponse response = matchScoreMapper.toResponse(matchScore);
        enrichResponseWithProviderDetails(response, provider);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  calculateMatchesForPatient
    //  Called when a patient profile is created or updated (FR-MATCH-01)
    //  Fans out across ALL active providers and upserts a MatchScore for each.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Recalculates match scores between the given patient and every active provider.
     *
     * Execution flow:
     *   1. Fetch the patient profile.
     *   2. Fetch all visible provider profiles (GET /api/v1/providers/all).
     *   3. For each provider: compute score via MatchingAlgorithmService,
     *      upsert the MatchScore row, publish a match.calculated event,
     *      and queue a threshold notification when score ≥ matchingThreshold.
     *
     * Runs @Async so the Kafka consumer calling this returns immediately.
     */
    @Async
    @Transactional
    public void calculateMatchesForPatient(UUID patientId) {
        log.info("calculateMatchesForPatient started: patientId={}", patientId);

        // 1. Fetch the patient
        PatientProfileDTO patient;
        try {
            patient = fetchPatientProfile(patientId);
        } catch (ResourceNotFoundException e) {
            log.warn("Patient profile not found, skipping match calculation: patientId={}", patientId);
            return;
        }

        // 2. Fetch all active providers
        List<ProviderProfileDTO> providers = fetchAllActiveProviders();
        if (providers.isEmpty()) {
            log.info("No active providers found; nothing to match against for patientId={}", patientId);
            return;
        }

        // 3. Score each provider
        int processed = 0;
        int notified  = 0;

        for (ProviderProfileDTO provider : providers) {
            try {
                MatchScore saved = computeAndPersist(patient, provider);
                if (saved.getScore().doubleValue() >= matchingThreshold) {
                    publishMatchCalculatedEvent(saved);
                    notified++;
                }
                processed++;
            } catch (Exception ex) {
                log.error("Error scoring pair patientId={} / providerId={}: {}",
                        patientId, provider.getId(), ex.getMessage(), ex);
            }
        }

        log.info("calculateMatchesForPatient complete: patientId={}, providers evaluated={}, notifications sent={}",
                patientId, processed, notified);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  calculateMatchesForProvider
    //  Called when a provider profile is created or updated (FR-MATCH-01)
    //  Fans out across ALL active patients and upserts a MatchScore for each.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Recalculates match scores between the given provider and every active patient.
     *
     * Execution flow:
     *   1. Fetch the provider profile.
     *   2. Fetch all patients who have given consent (GET /api/v1/patients/all).
     *   3. For each patient: compute score via MatchingAlgorithmService,
     *      upsert the MatchScore row, publish a match.calculated event,
     *      and queue a threshold notification when score ≥ matchingThreshold.
     *
     * Runs @Async so the Kafka consumer calling this returns immediately.
     */
    @Async
    @Transactional
    public void calculateMatchesForProvider(UUID providerId) {
        log.info("calculateMatchesForProvider started: providerId={}", providerId);

        // 1. Fetch the provider
        ProviderProfileDTO provider;
        try {
            provider = fetchProviderProfile(providerId);
        } catch (ResourceNotFoundException e) {
            log.warn("Provider profile not found, skipping match calculation: providerId={}", providerId);
            return;
        }

        // 2. Fetch all active (consent-given) patients
        List<PatientProfileDTO> patients = fetchAllActivePatients();
        if (patients.isEmpty()) {
            log.info("No active patients found; nothing to match against for providerId={}", providerId);
            return;
        }

        // 3. Score each patient
        int processed = 0;
        int notified  = 0;

        for (PatientProfileDTO patient : patients) {
            try {
                MatchScore saved = computeAndPersist(patient, provider);
                if (saved.getScore().doubleValue() >= matchingThreshold) {
                    publishMatchCalculatedEvent(saved);
                    notified++;
                }
                processed++;
            } catch (Exception ex) {
                log.error("Error scoring pair patientId={} / providerId={}: {}",
                        patient.getId(), providerId, ex.getMessage(), ex);
            }
        }

        log.info("calculateMatchesForProvider complete: providerId={}, patients evaluated={}, notifications sent={}",
                providerId, processed, notified);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  recalculate helpers (evict cache, delete stale rows, re-run)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    @CacheEvict(value = "matchScores", allEntries = true)
    public void recalculateMatchesForPatient(UUID patientId) {
        log.info("Recalculating (full reset) matches for patientId={}", patientId);
        matchScoreRepository.deleteByPatientId(patientId);
        calculateMatchesForPatient(patientId);
    }

    @Transactional
    @CacheEvict(value = "matchScores", allEntries = true)
    public void recalculateMatchesForProvider(UUID providerId) {
        log.info("Recalculating (full reset) matches for providerId={}", providerId);
        matchScoreRepository.deleteByProviderId(providerId);
        calculateMatchesForProvider(providerId);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Read methods (unchanged from existing implementation)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @Cacheable(value = "matchScores", key = "#patientId")
    public List<MatchScoreResponse> getMatchesForPatient(UUID patientId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        Page<MatchScore> matchesPage = matchScoreRepository.findByPatientIdOrderByScoreDesc(patientId, pageable);

        return matchesPage.getContent().stream()
                .map(match -> {
                    MatchScoreResponse response = matchScoreMapper.toResponse(match);
                    try {
                        ProviderProfileDTO provider = fetchProviderProfile(match.getProviderId());
                        enrichResponseWithProviderDetails(response, provider);
                    } catch (Exception e) {
                        log.warn("Could not fetch provider details: providerId={}", match.getProviderId(), e);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchScoreResponse> getMatchesForProvider(UUID providerId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        Page<MatchScore> matchesPage = matchScoreRepository.findByProviderIdOrderByScoreDesc(providerId, pageable);

        return matchesPage.getContent().stream()
                .map(matchScoreMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchScoreResponse> getTopMatchesForPatient(UUID patientId, int limit) {
        List<MatchScore> matches = matchScoreRepository.findByPatientIdAndScoreGreaterThanEqual(
                patientId, BigDecimal.valueOf(matchingThreshold));

        return matches.stream()
                .limit(limit)
                .map(match -> {
                    MatchScoreResponse response = matchScoreMapper.toResponse(match);
                    try {
                        ProviderProfileDTO provider = fetchProviderProfile(match.getProviderId());
                        enrichResponseWithProviderDetails(response, provider);
                    } catch (Exception e) {
                        log.warn("Could not fetch provider details: providerId={}", match.getProviderId(), e);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchScoreResponse getMatch(UUID patientId, UUID providerId) {
        MatchScore matchScore = matchScoreRepository
                .findByPatientIdAndProviderId(patientId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "patient+provider",
                        patientId + "/" + providerId));

        MatchScoreResponse response = matchScoreMapper.toResponse(matchScore);
        try {
            ProviderProfileDTO provider = fetchProviderProfile(providerId);
            enrichResponseWithProviderDetails(response, provider);
        } catch (Exception e) {
            log.warn("Could not fetch provider details: providerId={}", providerId, e);
        }
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Compute a score for a patient–provider pair and upsert the MatchScore row.
     * The explanation and breakdown are generated by MatchingAlgorithmService so
     * all scoring logic stays in one place.
     */
    private MatchScore computeAndPersist(PatientProfileDTO patient, ProviderProfileDTO provider) {

        BigDecimal          score       = matchingAlgorithmService.calculateMatchScore(patient, provider);
        Map<String, Object> explanation = matchingAlgorithmService.generateExplanation(patient, provider, score);
        Map<String, Object> breakdown   = matchingAlgorithmService.getScoreBreakdown(patient, provider);

        MatchScore matchScore = matchScoreRepository
                .findByPatientIdAndProviderId(patient.getId(), provider.getId())
                .orElse(MatchScore.builder()
                        .patientId(patient.getId())
                        .providerId(provider.getId())
                        .build());

        matchScore.setScore(score);
        matchScore.setExplanation(explanation);
        matchScore.setScoreBreakdown(breakdown);
        matchScore.setCalculatedAt(LocalDateTime.now());

        return matchScoreRepository.save(matchScore);
    }

    // ── Kafka event ───────────────────────────────────────────────────

    private void publishMatchCalculatedEvent(MatchScore matchScore) {
        // Avoid duplicate notifications for the same match
        if (matchNotificationRepository.existsByMatchIdAndNotificationSentTrue(matchScore.getId())) {
            log.debug("Notification already sent for matchId={}", matchScore.getId());
            return;
        }

        MatchCalculatedEvent event = MatchCalculatedEvent.builder()
                .eventType("match.calculated")
                .matchId(matchScore.getId())
                .patientId(matchScore.getPatientId())
                .providerId(matchScore.getProviderId())
                .score(matchScore.getScore())
                .timestamp(LocalDateTime.now())
                .build();

        matchingEventProducer.sendMatchCalculatedEvent(event);

        MatchNotification notification = MatchNotification.builder()
                .matchId(matchScore.getId())
                .notificationSent(true)
                .sentAt(LocalDateTime.now())
                .build();

        matchNotificationRepository.save(notification);
        log.debug("Threshold notification queued: matchId={}, score={}", matchScore.getId(), matchScore.getScore());
    }

    // ── Profile fetchers ──────────────────────────────────────────────

    private PatientProfileDTO fetchPatientProfile(UUID patientId) {
        try {
            ApiResponse<PatientProfileDTO> response = profileServiceClient.getPatientProfile(patientId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Patient profile", "id", patientId);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Patient profile", "id", patientId);
        } catch (FeignException e) {
            log.error("Feign error fetching patient profile: patientId={}", patientId, e);
            throw new RuntimeException("Failed to fetch patient profile from profile service", e);
        }
    }

    private ProviderProfileDTO fetchProviderProfile(UUID providerId) {
        try {
            ApiResponse<ProviderProfileDTO> response = profileServiceClient.getProviderProfile(providerId);
            log.info("Get profile by id: {}", response.getData().getId());
            log.info("Get profile latitude: {}", response.getData().getLatitude());
            log.info("Get profile longitude: {}", response.getData().getLongitude());
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Provider profile", "id", providerId);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Provider profile", "id", providerId);
        } catch (FeignException e) {
            log.error("Feign error fetching provider profile: providerId={}", providerId, e);
            throw new RuntimeException("Failed to fetch provider profile from profile service", e);
        }
    }

    private List<PatientProfileDTO> fetchAllActivePatients() {
        try {
            ApiResponse<List<PatientProfileDTO>> response = profileServiceClient.getAllActivePatients();
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData();
        } catch (FeignException e) {
            log.error("Feign error fetching all active patients", e);
            return Collections.emptyList();
        }
    }

    private List<ProviderProfileDTO> fetchAllActiveProviders() {
        try {
            ApiResponse<List<ProviderProfileDTO>> response = profileServiceClient.getAllActiveProviders();
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData();
        } catch (FeignException e) {
            log.error("Feign error fetching all active providers", e);
            return Collections.emptyList();
        }
    }

    // ── Response enrichment ───────────────────────────────────────────

    private void enrichResponseWithProviderDetails(MatchScoreResponse response, ProviderProfileDTO provider) {
        if (provider == null) return;
        response.setProviderName(provider.getFacilityName());
        response.setProviderType(provider.getProviderType() != null
                ? provider.getProviderType() : null);
    }
}
