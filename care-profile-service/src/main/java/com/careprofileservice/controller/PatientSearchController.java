package com.careprofileservice.controller;

import com.careprofileservice.dto.PatientProfileResponse;
import com.careprofileservice.dto.PatientSearchRequest;
import com.careprofileservice.service.PatientSearchService;
import com.carecommon.dto.ApiResponse;
import com.carecommon.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Allows subscribed providers to search the public patient directory.
 *
 * Access: RESIDENTIAL_PROVIDER or AMBULATORY_PROVIDER with an active subscription.
 *         Subscription gate is enforced by {@link PatientSearchService}.
 *
 * Base path: /api/v1/patients/search  (care-profile-service, port 8002)
 */
@RestController
@RequestMapping("/api/v1/patients/search")
@RequiredArgsConstructor
@Slf4j
public class PatientSearchController {

    private final PatientSearchService patientSearchService;

    /**
     * POST /api/v1/patients/search
     *
     * Body (all fields optional — acts as multi-filter):
     * {
     *   "careServiceTier": "PREMIUM",       // most important filter
     *   "careLevel": 3,
     *   "careType": ["RESIDENTIAL"],
     *   "region": "Bavaria",
     *   "maxDistanceKm": 50,               // requires provider lat/lon in header context
     *   "minAge": 60,
     *   "maxAge": 90,
     *   "page": 0,
     *   "size": 20
     * }
     *
     * Only patients with consentGiven=true and profilePublic=true are returned.
     */
    @PostMapping
    //@PreAuthorize("hasAnyRole('RESIDENTIAL_PROVIDER','AMBULATORY_PROVIDER')")
    public ResponseEntity<ApiResponse<PageResponse<PatientProfileResponse>>> searchPatients(
            @RequestHeader("X-User-Id") UUID providerUserId,
            @RequestBody PatientSearchRequest request) {

        log.info("Provider {} searching patients with filters: {}", providerUserId, request);

        Page<PatientProfileResponse> results = patientSearchService.searchPatients(
                providerUserId, request,
                PageRequest.of(
                        request.getPage() != null ? request.getPage() : 0,
                        request.getSize() != null ? request.getSize() : 20
                )
        );

        PageResponse<PatientProfileResponse> pageResponse = PageResponse.<PatientProfileResponse>builder()
                .content(results.getContent())
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Patient search results"));
    }
}