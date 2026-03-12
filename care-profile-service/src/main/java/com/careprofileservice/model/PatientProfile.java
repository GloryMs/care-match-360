package com.careprofileservice.model;

import com.carecommon.model.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles", schema = "care_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "care_level")
    private Integer careLevel;

    @Column(name = "care_type", columnDefinition = "text[]")
    private List<String> careType;

    @Type(JsonBinaryType.class)
    @Column(name = "lifestyle_attributes", columnDefinition = "jsonb")
    private Map<String, Object> lifestyleAttributes;

    @Type(JsonBinaryType.class)
    @Column(name = "medical_requirements", columnDefinition = "jsonb")
    private Map<String, Object> medicalRequirements;

    @Type(JsonBinaryType.class)
    @Column(name = "data_visibility", columnDefinition = "jsonb")
    private Map<String, Boolean> dataVisibility;

    @Column(name = "consent_given")
    @Builder.Default
    private Boolean consentGiven = false;

    // ── NEW FIELD ──────────────────────────────────────────────────────────────
    /**
     * The patient's preferred / affordable care service tier.
     * STANDARD  → basic statutory care (GKV-covered)
     * COMFORT   → mid-range with selected premium amenities
     * PREMIUM   → full VIP / luxury care with private-pay capacity
     * Replaces the former vague "financial capability" concept with a
     * domain-meaningful, actionable classification that providers can filter on.
     * This field is always PUBLIC — visible to all subscribed providers.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "care_service_tier", nullable = false)
    @Builder.Default
    private CareServiceTier careServiceTier = CareServiceTier.STANDARD;

    /**
     * Whether the full profile is publicly visible to subscribed providers.
     * Always TRUE — patient listings are public by design.
     * Kept as a column for potential future fine-grained visibility toggles.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean profilePublic = Boolean.TRUE;
    //
}
