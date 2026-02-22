package com.carematchservice.model;

import com.carecommon.model.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "offers", schema = "care_matching")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "match_id")
    private UUID matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OfferStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Type(JsonBinaryType.class)
    @Column(name = "availability_details", columnDefinition = "jsonb")
    private Map<String, Object> availabilityDetails;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public enum OfferStatus {
        DRAFT,
        SENT,
        VIEWED,
        ACCEPTED,
        REJECTED,
        EXPIRED
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canBeAccepted() {
        return status == OfferStatus.SENT || status == OfferStatus.VIEWED;
    }

    public boolean canBeRejected() {
        return status == OfferStatus.SENT || status == OfferStatus.VIEWED;
    }
}
