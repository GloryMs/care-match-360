package com.carematchservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a patient-initiated expression of interest in a specific provider.
 *
 * Lifecycle:  PENDING → ACCEPTED (when provider creates an offer)
 *                     → DECLINED (when provider declines)
 *
 * Schema: care_matching.care_requests
 */
@Entity
@Table(
    name = "care_requests",
    schema = "care_matching",
    uniqueConstraints = {
        // Prevent duplicate active requests from the same patient to the same provider
        @UniqueConstraint(
            name = "uq_care_request_patient_provider_pending",
            columnNames = {"patient_id", "provider_id", "status"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Patient profile UUID (from care-profile-service). */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /** Provider profile UUID (from care-profile-service). */
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "patient_message", length = 1000)
    private String patientMessage;

    /** Populated by the provider when declining. */
    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    /**
     * If the provider responded by creating an offer, that offer's UUID is stored here.
     * Allows the patient to navigate directly from the request to the offer.
     */
    @Column(name = "linked_offer_id")
    private UUID linkedOfferId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}