package com.carematchservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.CreateOfferFromSearchRequest;
import com.carematchservice.dto.CreateOfferRequest;
import com.carematchservice.dto.OfferHistoryResponse;
import com.carematchservice.dto.OfferResponse;
import com.carematchservice.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
//@Tag(name = "Offers", description = "Offer management endpoints")
@Slf4j
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    //@Operation(summary = "Create offer (provider only)")
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(
            @RequestHeader("X-Provider-Id") String providerId,
            @Valid @RequestBody CreateOfferRequest request) {

        OfferResponse offer = offerService.createOffer(UUID.fromString(providerId), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(offer, "Offer created successfully"));
    }

    @PutMapping("/{offerId}/send")
    //@Operation(summary = "Send offer to patient (provider only)")
    public ResponseEntity<ApiResponse<OfferResponse>> sendOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Provider-Id") String providerId) {

        OfferResponse offer = offerService.sendOffer(offerId, UUID.fromString(providerId));
        return ResponseEntity.ok(ApiResponse.success(offer, "Offer sent successfully"));
    }

    @PutMapping("/{offerId}/accept")
    //@Operation(summary = "Accept offer (patient only)")
    public ResponseEntity<ApiResponse<OfferResponse>> acceptOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Patient-Id") String patientId) {

        OfferResponse offer = offerService.acceptOffer(offerId, UUID.fromString(patientId));
        return ResponseEntity.ok(ApiResponse.success(offer, "Offer accepted successfully"));
    }

    @PutMapping("/{offerId}/reject")
    //@Operation(summary = "Reject offer (patient only)")
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-Patient-Id") String patientId) {

        OfferResponse offer = offerService.rejectOffer(offerId, UUID.fromString(patientId));
        return ResponseEntity.ok(ApiResponse.success(offer, "Offer rejected successfully"));
    }

    @GetMapping("/{offerId}")
    //@Operation(summary = "Get offer by ID")
    public ResponseEntity<ApiResponse<OfferResponse>> getOffer(@PathVariable UUID offerId) {
        OfferResponse offer = offerService.getOffer(offerId);
        return ResponseEntity.ok(ApiResponse.success(offer));
    }

    @GetMapping("/patient/{patientId}")
    //@Operation(summary = "Get all offers for a patient")
    public ResponseEntity<ApiResponse<List<OfferResponse>>> getOffersForPatient(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<OfferResponse> offers = offerService.getOffersForPatient(patientId, page, size);
        return ResponseEntity.ok(ApiResponse.success(offers));
    }

    @GetMapping("/provider/{providerId}")
    //@Operation(summary = "Get all offers for a provider")
    public ResponseEntity<ApiResponse<List<OfferResponse>>> getOffersForProvider(
            @PathVariable UUID providerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<OfferResponse> offers = offerService.getOffersForProvider(providerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(offers));
    }

    @GetMapping("/{offerId}/history")
    //@Operation(summary = "Get offer history")
    public ResponseEntity<ApiResponse<List<OfferHistoryResponse>>> getOfferHistory(
            @PathVariable UUID offerId) {

        List<OfferHistoryResponse> history = offerService.getOfferHistory(offerId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // ── NEW endpoint ──────────────────────────────────────────────────────────

    /**
     * POST /api/v1/offers/from-patient-search
     *
     * Provider sends an offer directly to a patient found via the patient search directory.
     * This combines the create + send steps in one call for a streamlined UX.
     *
     * Access: RESIDENTIAL_PROVIDER or AMBULATORY_PROVIDER with active subscription.
     *
     * Body:
     * {
     *   "patientProfileId": "uuid-of-patient",
     *   "message": "We are pleased to offer you a place in our premium ward...",
     *   "proposedStartDate": "2026-04-01",
     *   "monthlyFee": 3500.00,
     *   "includedServices": ["Meals", "Laundry", "Physiotherapy"],
     *   "validUntil": "2026-03-20"
     * }
     *
     * Returns the created + sent offer.
     * Status transitions: DRAFT → SENT in a single operation.
     */
    @PostMapping("/from-patient-search")
    //@PreAuthorize("hasAnyRole('RESIDENTIAL_PROVIDER','AMBULATORY_PROVIDER')")
    public ResponseEntity<ApiResponse<OfferResponse>> sendOfferFromPatientSearch(
            @RequestHeader("X-User-Id") UUID providerUserId,
            @Valid @RequestBody CreateOfferFromSearchRequest request) {

        log.info("Provider {} sending offer to patient {} from search", providerUserId,
                request.getPatientProfileId());

        OfferResponse offer = offerService.createAndSendOfferFromSearch(providerUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(offer, "Offer sent to patient"));
    }
}
