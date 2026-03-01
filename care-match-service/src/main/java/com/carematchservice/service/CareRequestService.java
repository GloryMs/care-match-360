package com.carematchservice.service;

import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.kafkaEvents.CareRequestDeclinedEvent;
import com.carecommon.kafkaEvents.CareRequestSubmittedEvent;
import com.carematchservice.dto.*;
import com.carematchservice.feign.BillingServiceClient;
import com.carematchservice.feign.ProfileServiceClient;
import com.carematchservice.kafka.MatchingEventProducer;
import com.carematchservice.model.CareRequest;
import com.carematchservice.model.Offer;
import com.carematchservice.repository.CareRequestRepository;
import com.carematchservice.repository.OfferRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareRequestService {

    private final CareRequestRepository careRequestRepository;
    private final OfferRepository        offerRepository;
    private final ProfileServiceClient   profileServiceClient;
    private final BillingServiceClient   billingServiceClient;
    private final MatchingEventProducer  eventProducer;

    // ── Patient submits a care request (Requirement 4) ────────────────────────

    @Transactional
    public CareRequestResponse submitRequest(UUID patientId, CreateCareRequestRequest request) {
        log.info("Care request submitted: patientId={}, providerId={}", patientId, request.getProviderId());

        // Prevent duplicate active requests
        careRequestRepository.findByPatientIdAndProviderIdAndStatus(
                patientId, request.getProviderId(), CareRequest.RequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ValidationException(
                            "You already have a pending request to this provider. " +
                            "Please wait for their response before submitting again.");
                });

        CareRequest careRequest = CareRequest.builder()
                .patientId(patientId)
                .providerId(request.getProviderId())
                .patientMessage(request.getPatientMessage())
                .status(CareRequest.RequestStatus.PENDING)
                .build();

        careRequest = careRequestRepository.save(careRequest);

        // Publish Kafka event → care-notification-service will email the provider
        try {
            CareRequestSubmittedEvent event = CareRequestSubmittedEvent.builder()
                    .eventType("care-request.submitted")
                    .requestId(careRequest.getId())
                    .patientId(patientId)
                    .providerId(request.getProviderId())
                    .patientMessage(request.getPatientMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            eventProducer.sendCareRequestSubmittedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish CareRequestSubmittedEvent: requestId={}", careRequest.getId(), e);
            // Non-blocking — request is already saved
        }

        return toResponse(careRequest);
    }

    // ── Patient views their requests (Requirement 4 follow-up) ───────────────

    @Transactional(readOnly = true)
    public Page<CareRequestResponse> getPatientRequests(
            UUID patientId, String status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CareRequest> requestPage;

        if (status != null) {
            CareRequest.RequestStatus s = CareRequest.RequestStatus.valueOf(status.toUpperCase());
            requestPage = careRequestRepository.findByPatientIdAndStatus(patientId, s, pageable);
        } else {
            requestPage = careRequestRepository.findByPatientId(patientId, pageable);
        }

        return requestPage.map(this::toEnrichedResponse);
    }

    // ── Provider views incoming requests — inbox (Requirement 5) ─────────────

    @Transactional(readOnly = true)
    public Page<CareRequestResponse> getProviderRequests(
            UUID providerId, String status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CareRequest> requestPage;

        if (status != null) {
            CareRequest.RequestStatus s = CareRequest.RequestStatus.valueOf(status.toUpperCase());
            requestPage = careRequestRepository.findByProviderIdAndStatus(providerId, s, pageable);
        } else {
            requestPage = careRequestRepository.findByProviderId(providerId, pageable);
        }

        return requestPage.map(this::toResponse);
    }

    // ── Provider declines a request (Requirement 5 — apologize) ──────────────

    @Transactional
    public CareRequestResponse declineRequest(
            UUID requestId, UUID providerId, DeclineCareRequestRequest body) {

        CareRequest careRequest = careRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CareRequest", "id", requestId));

        if (!careRequest.getProviderId().equals(providerId)) {
            throw new SecurityException("You are not authorized to decline this request.");
        }
        if (careRequest.getStatus() != CareRequest.RequestStatus.PENDING) {
            throw new ValidationException("Only PENDING requests can be declined.");
        }

        careRequest.setStatus(CareRequest.RequestStatus.DECLINED);
        careRequest.setDeclineReason(body.getDeclineReason());
        careRequest.setRespondedAt(LocalDateTime.now());
        careRequest = careRequestRepository.save(careRequest);

        // Publish Kafka event → care-notification-service will email the patient
        try {
            // Resolve provider name for the notification email
            String providerName = resolveProviderName(providerId);

            CareRequestDeclinedEvent event = CareRequestDeclinedEvent.builder()
                    .eventType("care-request.declined")
                    .requestId(careRequest.getId())
                    .patientId(careRequest.getPatientId())
                    .providerId(providerId)
                    .providerName(providerName)
                    .declineReason(body.getDeclineReason())
                    .timestamp(LocalDateTime.now())
                    .build();
            eventProducer.sendCareRequestDeclinedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish CareRequestDeclinedEvent: requestId={}", requestId, e);
        }

        return toResponse(careRequest);
    }

    // ── Called by OfferService when a provider creates an offer in response ───

    @Transactional
    public void linkOfferToRequest(UUID requestId, UUID offerId) {
        careRequestRepository.findById(requestId).ifPresent(cr -> {
            cr.setLinkedOfferId(offerId);
            cr.setStatus(CareRequest.RequestStatus.ACCEPTED);
            cr.setRespondedAt(LocalDateTime.now());
            careRequestRepository.save(cr);
            log.info("CareRequest {} linked to offer {} and marked ACCEPTED", requestId, offerId);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CareRequestResponse toResponse(CareRequest cr) {
        return CareRequestResponse.builder()
                .id(cr.getId())
                .patientId(cr.getPatientId())
                .providerId(cr.getProviderId())
                .status(cr.getStatus().name())
                .patientMessage(cr.getPatientMessage())
                .declineReason(cr.getDeclineReason())
                .linkedOfferId(cr.getLinkedOfferId())
                .createdAt(cr.getCreatedAt())
                .updatedAt(cr.getUpdatedAt())
                .respondedAt(cr.getRespondedAt())
                .build();
    }

    private CareRequestResponse toEnrichedResponse(CareRequest cr) {
        CareRequestResponse resp = toResponse(cr);
        try {
            // Enrich with provider name/type/address from profile service
            var providerProfile = profileServiceClient.getProviderProfile(cr.getProviderId());
            if (providerProfile != null && providerProfile.getData() != null) {
                resp.setProviderName(providerProfile.getData().getFacilityName());
                resp.setProviderType(providerProfile.getData().getProviderType());
                resp.setProviderAddress(providerProfile.getData().getAddress());
            }
        } catch (Exception e) {
            log.warn("Could not enrich care request {} with provider details: {}", cr.getId(), e.getMessage());
        }
        return resp;
    }

    private String resolveProviderName(UUID providerId) {
        try {
            var profile = profileServiceClient.getProviderProfile(providerId);
            return profile.getData() != null ? profile.getData().getFacilityName() : "Your provider";
        } catch (Exception e) {
            return "Your provider";
        }
    }
}