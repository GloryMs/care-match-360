package com.carematchservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.CreateOfferRequest;
import com.carematchservice.dto.OfferHistoryResponse;
import com.carematchservice.dto.OfferResponse;
import com.carematchservice.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
//@Tag(name = "Offers", description = "Offer management endpoints")
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    //@Operation(summary = "Create offer (provider only)")
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOfferRequest request) {

        // In production, you'd get provider profile ID from user ID via Profile Service
        // For now, we assume userId corresponds to provider profile ID
        UUID providerId = UUID.fromString(userId);

        OfferResponse offer = offerService.createOffer(providerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(offer, "Offer created successfully"));
    }

    @PutMapping("/{offerId}/send")
    //@Operation(summary = "Send offer to patient (provider only)")
    public ResponseEntity<ApiResponse<OfferResponse>> sendOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-User-Id") String userId) {

        UUID providerId = UUID.fromString(userId);
        OfferResponse offer = offerService.sendOffer(offerId, providerId);
        return ResponseEntity.ok(ApiResponse.success(offer, "Offer sent successfully"));
    }

    @PutMapping("/{offerId}/accept")
    //@Operation(summary = "Accept offer (patient only)")
    public ResponseEntity<ApiResponse<OfferResponse>> acceptOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-User-Id") String userId) {

        UUID patientId = UUID.fromString(userId);
        OfferResponse offer = offerService.acceptOffer(offerId, patientId);
        return ResponseEntity.ok(ApiResponse.success(offer, "Offer accepted successfully"));
    }

    @PutMapping("/{offerId}/reject")
    //@Operation(summary = "Reject offer (patient only)")
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOffer(
            @PathVariable UUID offerId,
            @RequestHeader("X-User-Id") String userId) {

        UUID patientId = UUID.fromString(userId);
        OfferResponse offer = offerService.rejectOffer(offerId, patientId);
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
}
