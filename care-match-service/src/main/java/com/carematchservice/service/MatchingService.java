package com.carematchservice.service;



import com.carecommon.dto.ApiResponse;
import com.carecommon.exception.ResourceNotFoundException;
import com.carematchservice.dto.MatchScoreResponse;
import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import com.carematchservice.feign.ProfileServiceClient;
import com.carematchservice.kafka.events.MatchCalculatedEvent;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final MatchScoreRepository matchScoreRepository;
    private final MatchNotificationRepository matchNotificationRepository;
    private final ProfileServiceClient profileServiceClient;
    private final MatchingAlgorithmService matchingAlgorithmService;
    private final MatchingEventProducer matchingEventProducer;
    private final MatchScoreMapper matchScoreMapper;

    @Value("${app.matching.threshold}")
    private int matchingThreshold;

    @Transactional
    public MatchScoreResponse calculateMatch(UUID patientId, UUID providerId) {
        log.info("Calculating match: patientId={}, providerId={}", patientId, providerId);

        // Fetch profiles
        PatientProfileDTO patient = fetchPatientProfile(patientId);
        ProviderProfileDTO provider = fetchProviderProfile(providerId);

        // Calculate score
        BigDecimal score = matchingAlgorithmService.calculateMatchScore(patient, provider);
        Map<String, Object> explanation = matchingAlgorithmService.generateExplanation(patient, provider, score);
        Map<String, Object> breakdown = matchingAlgorithmService.getScoreBreakdown(patient, provider);

        // Save or update match score
        MatchScore matchScore = matchScoreRepository.findByPatientIdAndProviderId(patientId, providerId)
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

        // If score meets threshold, trigger notification
        if (score.doubleValue() >= matchingThreshold) {
            publishMatchCalculatedEvent(matchScore);
        }

        // Map to response
        MatchScoreResponse response = matchScoreMapper.toResponse(matchScore);
        enrichResponseWithProviderDetails(response, provider);

        return response;
    }

    @Async
    @Transactional
    public void calculateMatchesForPatient(UUID patientId) {
        log.info("Calculating matches for patient: patientId={}", patientId);

        try {
            PatientProfileDTO patient = fetchPatientProfile(patientId);

            // Fetch all providers (in production, you'd do this in batches)
            // For now, we'll assume a reasonable number of providers
            // In reality, you might want to filter by region first

            // TODO: Implement batch fetching of providers
            // For this example, we'll skip the actual implementation
            // and just log the action

            log.info("Match calculation for patient {} would fetch all providers and calculate scores", patientId);

        } catch (Exception e) {
            log.error("Error calculating matches for patient: patientId={}", patientId, e);
        }
    }

    @Async
    @Transactional
    public void calculateMatchesForProvider(UUID providerId) {
        log.info("Calculating matches for provider: providerId={}", providerId);

        try {
            ProviderProfileDTO provider = fetchProviderProfile(providerId);

            // Similar to calculateMatchesForPatient
            log.info("Match calculation for provider {} would fetch all patients and calculate scores", providerId);

        } catch (Exception e) {
            log.error("Error calculating matches for provider: providerId={}", providerId, e);
        }
    }

    @Transactional
    @CacheEvict(value = "matchScores", allEntries = true)
    public void recalculateMatchesForPatient(UUID patientId) {
        log.info("Recalculating matches for patient: patientId={}", patientId);

        // Delete existing matches
        matchScoreRepository.deleteByPatientId(patientId);

        // Recalculate
        calculateMatchesForPatient(patientId);
    }

    @Transactional
    @CacheEvict(value = "matchScores", allEntries = true)
    public void recalculateMatchesForProvider(UUID providerId) {
        log.info("Recalculating matches for provider: providerId={}", providerId);

        // Delete existing matches
        matchScoreRepository.deleteByProviderId(providerId);

        // Recalculate
        calculateMatchesForProvider(providerId);
    }

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
                patientId,
                BigDecimal.valueOf(matchingThreshold)
        );

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
        MatchScore matchScore = matchScoreRepository.findByPatientIdAndProviderId(patientId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found for patient and provider"));

        MatchScoreResponse response = matchScoreMapper.toResponse(matchScore);

        try {
            ProviderProfileDTO provider = fetchProviderProfile(providerId);
            enrichResponseWithProviderDetails(response, provider);
        } catch (Exception e) {
            log.warn("Could not fetch provider details: providerId={}", providerId, e);
        }

        return response;
    }

    private void publishMatchCalculatedEvent(MatchScore matchScore) {
        // Check if notification already sent
        if (matchNotificationRepository.existsByMatchIdAndNotificationSentTrue(matchScore.getId())) {
            log.debug("Notification already sent for match: matchId={}", matchScore.getId());
            return;
        }

        // Publish event
        MatchCalculatedEvent event = MatchCalculatedEvent.builder()
                .eventType("match.calculated")
                .matchId(matchScore.getId())
                .patientId(matchScore.getPatientId())
                .providerId(matchScore.getProviderId())
                .score(matchScore.getScore())
                .timestamp(LocalDateTime.now())
                .build();

        matchingEventProducer.sendMatchCalculatedEvent(event);

        // Save notification record
        MatchNotification notification = MatchNotification.builder()
                .matchId(matchScore.getId())
                .notificationSent(true)
                .sentAt(LocalDateTime.now())
                .build();

        matchNotificationRepository.save(notification);
    }

    private PatientProfileDTO fetchPatientProfile(UUID patientId) {
        try {
            ApiResponse<PatientProfileDTO> response = profileServiceClient.getPatientProfile(patientId);
            if (response.getData() == null) {
                throw new ResourceNotFoundException("Patient profile", "id", patientId);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Patient profile", "id", patientId);
        } catch (FeignException e) {
            log.error("Error fetching patient profile: patientId={}", patientId, e);
            throw new RuntimeException("Failed to fetch patient profile", e);
        }
    }

    private ProviderProfileDTO fetchProviderProfile(UUID providerId) {
        try {
            ApiResponse<ProviderProfileDTO> response = profileServiceClient.getProviderProfile(providerId);
            if (response.getData() == null) {
                throw new ResourceNotFoundException("Provider profile", "id", providerId);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Provider profile", "id", providerId);
        } catch (FeignException e) {
            log.error("Error fetching provider profile: providerId={}", providerId, e);
            throw new RuntimeException("Failed to fetch provider profile", e);
        }
    }

    private void enrichResponseWithProviderDetails(MatchScoreResponse response, ProviderProfileDTO provider) {
        response.setProviderName(provider.getFacilityName());
        response.setProviderType(provider.getProviderType());
        response.setProviderAddress(provider.getAddress());
    }
}
