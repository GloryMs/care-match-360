package com.careprofileservice.service;

import com.carecommon.dto.ApiResponse;
import com.careprofileservice.dto.PatientProfileResponse;
import com.careprofileservice.dto.PatientSearchRequest;
import com.careprofileservice.dto.SubscriptionStatusDTO;
import com.careprofileservice.feign.BillingServiceClient;
import com.careprofileservice.model.PatientProfile;
import com.careprofileservice.model.ProviderProfile;
import com.careprofileservice.repository.PatientProfileRepository;
import com.careprofileservice.repository.ProviderProfileRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientSearchService {

    private final PatientProfileRepository  patientRepo;
    private final ProviderProfileRepository providerRepo;
    private final BillingServiceClient billingServiceClient;

    /**
     * Search public patient profiles.
     * Gate: provider must have an active subscription (ACTIVE or TRIALING).
     */
    public Page<PatientProfileResponse> searchPatients(
            UUID providerUserId,
            PatientSearchRequest request,
            Pageable pageable) {

        // 1. Resolve provider profile
        ProviderProfile provider = providerRepo.findByUserId(providerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Provider profile not found"));

        // 2. Subscription gate — via Feign to care-billing-service
        if (!checkSubscription(provider.getId())) {
            throw new IllegalStateException(
                    "An active subscription is required to search patients. " +
                            "Please subscribe at /subscription.");
        }

        // 3. Fetch all consent-given, public patients
        List<PatientProfile> candidates = patientRepo.findAllByConsentGivenTrueAndProfilePublicTrue();

        // 4. Apply filters
        List<PatientProfile> filtered = candidates.stream()
                .filter(p -> filterByTier(p, request))
                .filter(p -> filterByCareLevel(p, request))
                .filter(p -> filterByCareType(p, request))
                .filter(p -> filterByRegion(p, request))
                .filter(p -> filterByAge(p, request))
                .filter(p -> filterByDistance(p, provider, request))
                .toList();

        // 5. Apply pagination manually (use JPA Specification for production-grade filtering)
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), filtered.size());
        List<PatientProfile> paged = start > filtered.size()
                ? List.of()
                : filtered.subList(start, end);

        List<PatientProfileResponse> responses = paged.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, filtered.size());
    }

    // ── Filter predicates ─────────────────────────────────────────────────────

    private boolean filterByTier(PatientProfile p, PatientSearchRequest r) {
        if (r.getCareServiceTier() == null) return true;
        return r.getCareServiceTier().equals(p.getCareServiceTier());
    }

    private boolean filterByCareLevel(PatientProfile p, PatientSearchRequest r) {
        if (r.getCareLevel() == null) return true;
        return r.getCareLevel().equals(p.getCareLevel());
    }

    private boolean filterByCareType(PatientProfile p, PatientSearchRequest r) {
        if (r.getCareType() == null || r.getCareType().isEmpty()) return true;
        if (p.getCareType() == null) return false;
        return p.getCareType().stream().anyMatch(r.getCareType()::contains);
    }

    private boolean filterByRegion(PatientProfile p, PatientSearchRequest r) {
        if (r.getRegion() == null || r.getRegion().isBlank()) return true;
        if (p.getRegion() == null) return false;
        return p.getRegion().toLowerCase().contains(r.getRegion().toLowerCase());
    }

    private boolean filterByAge(PatientProfile p, PatientSearchRequest r) {
        if (p.getAge() == null) return true;
        if (r.getMinAge() != null && p.getAge() < r.getMinAge()) return false;
        if (r.getMaxAge() != null && p.getAge() > r.getMaxAge()) return false;
        return true;
    }

    private boolean filterByDistance(PatientProfile p, ProviderProfile prov, PatientSearchRequest r) {
        if (r.getMaxDistanceKm() == null) return true;
        if (p.getLocation() == null ) return true;
        if (prov.getLocation() == null ) return true;
        double dist = haversineKm(p.getLocation().getY(), p.getLocation().getX(),
                prov.getLocation().getY(), prov.getLocation().getX());
        return dist <= r.getMaxDistanceKm();
    }

    // ── Haversine distance ────────────────────────────────────────────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ── Subscription check via Feign ──────────────────────────────────────────

    /**
     * Calls care-billing-service via the registered Feign client.
     * Returns true only when the subscription status is ACTIVE or TRIALING.
     * Any Feign exception is treated as "no active subscription" to fail safely.
     */
    private boolean checkSubscription(UUID providerId) {
        try {
            ApiResponse<SubscriptionStatusDTO> response =
                    billingServiceClient.getProviderSubscriptionStatus(providerId);

            if (response == null || response.getData() == null) {
                log.warn("Null subscription status response for provider {}", providerId);
                return false;
            }
            return Boolean.TRUE.equals(response.getData().getIsActive());

        } catch (FeignException.NotFound e) {
            // Provider has no subscription record yet
            log.info("No subscription found for provider {}", providerId);
            return false;
        } catch (FeignException e) {
            log.error("Feign error checking subscription for provider {}: status={}, message={}",
                    providerId, e.status(), e.getMessage());
            return false;
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private PatientProfileResponse toResponse(PatientProfile p) {
        return PatientProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .age(p.getAge())
                .gender(p.getGender())
                .region(p.getRegion())
                .latitude(p.getLocation().getY())
                .longitude(p.getLocation().getX())
                .careLevel(p.getCareLevel())
                .careType(p.getCareType())
                .lifestyleAttributes(p.getLifestyleAttributes())
                .medicalRequirements(p.getMedicalRequirements())
                .dataVisibility(p.getDataVisibility())
                .consentGiven(p.getConsentGiven())
                .careServiceTier(p.getCareServiceTier())
                .profilePublic(p.getProfilePublic())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}