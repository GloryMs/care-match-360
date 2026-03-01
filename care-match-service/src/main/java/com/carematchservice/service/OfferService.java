package com.carematchservice.service;

import com.carecommon.dto.ApiResponse;
import com.carecommon.exception.ResourceNotFoundException;
import com.carematchservice.dto.CreateOfferRequest;
import com.carematchservice.dto.OfferHistoryResponse;
import com.carematchservice.dto.OfferResponse;
import com.carematchservice.dto.SubscriptionStatusDTO;
import com.carematchservice.feign.BillingServiceClient;
import com.carematchservice.kafka.MatchingEventProducer;
import com.carecommon.kafkaEvents.OfferAcceptedEvent;
import com.carecommon.kafkaEvents.OfferRejectedEvent;
import com.carecommon.kafkaEvents.OfferSentEvent;
import com.carematchservice.mapper.OfferHistoryMapper;
import com.carematchservice.mapper.OfferMapper;
import com.carematchservice.model.MatchScore;
import com.carematchservice.model.Offer;
import com.carematchservice.model.OfferHistory;
import com.carematchservice.repository.MatchScoreRepository;
import com.carematchservice.repository.OfferHistoryRepository;
import com.carematchservice.repository.OfferRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferHistoryRepository offerHistoryRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final OfferMapper offerMapper;
    private final OfferHistoryMapper offerHistoryMapper;
    private final MatchingEventProducer matchingEventProducer;
    private final BillingServiceClient billingServiceClient;
    private final CareRequestService   careRequestService;

    @Value("${app.offer.expiration-days}")
    private int offerExpirationDays;

    @Transactional
    public OfferResponse createOffer(UUID providerId, CreateOfferRequest request) {
        log.info("Creating offer: providerId={}, patientId={}", providerId, request.getPatientId());

        checkSubscription(providerId);
        // Check if match exists
        MatchScore matchScore = matchScoreRepository.findByPatientIdAndProviderId(
                request.getPatientId(), providerId
        ).orElse(null);

        // Create offer
        Offer offer = offerMapper.toEntity(request);
        offer.setProviderId(providerId);
        offer.setStatus(Offer.OfferStatus.DRAFT);
        offer.setMatchId(matchScore != null ? matchScore.getId() : null);
        offer.setExpiresAt(LocalDateTime.now().plusDays(offerExpirationDays));

        offer = offerRepository.save(offer);

        if (request.getCareRequestId() != null) {
            careRequestService.linkOfferToRequest(request.getCareRequestId(), offer.getId());
        }

        log.info("Offer created: offerId={}", offer.getId());

        // Record history
        recordOfferHistory(offer.getId(), null, Offer.OfferStatus.DRAFT.name(), providerId, "Offer created");

        OfferResponse response = offerMapper.toResponse(offer);
        if (matchScore != null) {
            response.setMatchScore(matchScore.getScore().doubleValue());
        }

        return response;
    }

    @Transactional
    public OfferResponse sendOffer(UUID offerId, UUID providerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", "id", offerId));

        checkSubscription(providerId);

        // Validate provider owns this offer
        if (!offer.getProviderId().equals(providerId)) {
            throw new ValidationException("You don't have permission to send this offer");
        }

        // Validate status
        if (offer.getStatus() != Offer.OfferStatus.DRAFT) {
            throw new ValidationException("Only draft offers can be sent");
        }

        // Update status
        Offer.OfferStatus oldStatus = offer.getStatus();
        offer.setStatus(Offer.OfferStatus.SENT);
        offer = offerRepository.save(offer);

        log.info("Offer sent: offerId={}", offerId);

        // Record history
        recordOfferHistory(offerId, oldStatus.name(), Offer.OfferStatus.SENT.name(), providerId, "Offer sent to patient");

        // Publish event
        OfferSentEvent event = OfferSentEvent.builder()
                .eventType("offer.sent")
                .offerId(offerId)
                .patientId(offer.getPatientId())
                .providerId(providerId)
                .timestamp(LocalDateTime.now())
                .build();

        matchingEventProducer.sendOfferSentEvent(event);

        return offerMapper.toResponse(offer);
    }

    private void checkSubscription(UUID providerId) {
        try {
            ApiResponse<SubscriptionStatusDTO> resp =
                    billingServiceClient.getProviderSubscriptionStatus(providerId);
            if (resp == null || resp.getData() == null || !resp.getData().isActive()) {
                throw new ValidationException(
                        "An active subscription is required to create or send offers. " +
                                "Please subscribe or renew your plan.");
            }
        } catch (ValidationException ve) {
            throw ve;   // re-throw our own
        } catch (Exception e) {
            // If billing service is unreachable, fail open with a warning (configurable behaviour)
            log.warn("Could not verify subscription for provider {}. Allowing offer creation. Error: {}",
                    providerId, e.getMessage());
        }
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId, UUID patientId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", "id", offerId));

        // Validate patient owns this offer
        if (!offer.getPatientId().equals(patientId)) {
            throw new ValidationException("You don't have permission to accept this offer");
        }

        // Validate can be accepted
        if (!offer.canBeAccepted()) {
            throw new ValidationException("Offer cannot be accepted in current status: " + offer.getStatus());
        }

        // Check if expired
        if (offer.isExpired()) {
            offer.setStatus(Offer.OfferStatus.EXPIRED);
            offerRepository.save(offer);
            throw new ValidationException("Offer has expired");
        }

        // Update status
        Offer.OfferStatus oldStatus = offer.getStatus();
        offer.setStatus(Offer.OfferStatus.ACCEPTED);
        offer = offerRepository.save(offer);

        log.info("Offer accepted: offerId={}", offerId);

        // Record history
        recordOfferHistory(offerId, oldStatus.name(), Offer.OfferStatus.ACCEPTED.name(), patientId, "Offer accepted by patient");

        // Publish event
        OfferAcceptedEvent event = OfferAcceptedEvent.builder()
                .eventType("offer.accepted")
                .offerId(offerId)
                .patientId(patientId)
                .providerId(offer.getProviderId())
                .timestamp(LocalDateTime.now())
                .build();

        matchingEventProducer.sendOfferAcceptedEvent(event);

        return offerMapper.toResponse(offer);
    }

    @Transactional
    public OfferResponse rejectOffer(UUID offerId, UUID patientId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", "id", offerId));

        // Validate patient owns this offer
        if (!offer.getPatientId().equals(patientId)) {
            throw new ValidationException("You don't have permission to reject this offer");
        }

        // Validate can be rejected
        if (!offer.canBeRejected()) {
            throw new ValidationException("Offer cannot be rejected in current status: " + offer.getStatus());
        }

        // Update status
        Offer.OfferStatus oldStatus = offer.getStatus();
        offer.setStatus(Offer.OfferStatus.REJECTED);
        offer = offerRepository.save(offer);

        log.info("Offer rejected: offerId={}", offerId);

        // Record history
        recordOfferHistory(offerId, oldStatus.name(), Offer.OfferStatus.REJECTED.name(), patientId, "Offer rejected by patient");

        // Publish event
        OfferRejectedEvent event = OfferRejectedEvent.builder()
                .eventType("offer.rejected")
                .offerId(offerId)
                .patientId(patientId)
                .providerId(offer.getProviderId())
                .timestamp(LocalDateTime.now())
                .build();

        matchingEventProducer.sendOfferRejectedEvent(event);

        return offerMapper.toResponse(offer);
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", "id", offerId));

        return offerMapper.toResponse(offer);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersForPatient(UUID patientId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        Page<Offer> offersPage = offerRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);

        return offersPage.getContent().stream()
                .map(offerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersForProvider(UUID providerId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        Page<Offer> offersPage = offerRepository.findByProviderIdOrderByCreatedAtDesc(providerId, pageable);

        return offersPage.getContent().stream()
                .map(offerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferHistoryResponse> getOfferHistory(UUID offerId) {
        List<OfferHistory> history = offerHistoryRepository.findByOfferIdOrderByChangedAtDesc(offerId);

        return history.stream()
                .map(offerHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void expireOldOffers() {
        log.info("Running scheduled offer expiration job");

        List<Offer> expiredOffers = offerRepository.findExpiredOffers(
                Offer.OfferStatus.SENT,
                LocalDateTime.now()
        );

        for (Offer offer : expiredOffers) {
            Offer.OfferStatus oldStatus = offer.getStatus();
            offer.setStatus(Offer.OfferStatus.EXPIRED);
            offerRepository.save(offer);

            recordOfferHistory(
                    offer.getId(),
                    oldStatus.name(),
                    Offer.OfferStatus.EXPIRED.name(),
                    null,
                    "Offer expired automatically"
            );

            log.info("Offer expired: offerId={}", offer.getId());
        }

        log.info("Expired {} offers", expiredOffers.size());
    }

    private void recordOfferHistory(UUID offerId, String oldStatus, String newStatus, UUID changedBy, String notes) {
        OfferHistory history = OfferHistory.builder()
                .offerId(offerId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes(notes)
                .build();

        offerHistoryRepository.save(history);
    }
}
