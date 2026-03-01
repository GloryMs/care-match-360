package com.careprofileservice.model;


import com.carecommon.model.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "provider_profiles", schema = "care_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "facility_name", length = 255)
    private String facilityName;

    @Convert(converter = ProviderTypeConverter.class)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "address", length = 500)
    private String address;

    // Residential specific
    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "available_rooms")
    private Integer availableRooms;

    @Type(JsonBinaryType.class)
    @Column(name = "room_types", columnDefinition = "jsonb")
    private Map<String, Integer> roomTypes;

    // Ambulatory specific
    @Column(name = "service_radius")
    private Integer serviceRadius;

    @Column(name = "max_daily_patients")
    private Integer maxDailyPatients;

    // Common fields
    @Type(JsonBinaryType.class)
    @Column(name = "specializations", columnDefinition = "jsonb")
    private List<String> specializations;

    @Column(name = "staff_count")
    private Integer staffCount;

    @Column(name = "staff_to_patient_ratio", precision = 5, scale = 2)
    private BigDecimal staffToPatientRatio;

    @Type(JsonBinaryType.class)
    @Column(name = "availability", columnDefinition = "jsonb")
    private Map<String, Object> availability;

    @Type(JsonBinaryType.class)
    @Column(name = "quality_indicators", columnDefinition = "jsonb")
    private Map<String, Object> qualityIndicators;

    @Column(name = "intro_video_url", length = 500)
    private String introVideoUrl;

    @Type(JsonBinaryType.class)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images;

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @Type(JsonBinaryType.class)
    @Column(name = "accepted_care_levels", columnDefinition = "jsonb")
    private List<Integer> acceptedCareLevels;

    @Type(JsonBinaryType.class)
    @Column(name = "lifestyle_options", columnDefinition = "jsonb")
    private Map<String, Object> lifestyleOptions;


    public enum ProviderType {
        RESIDENTIAL,
        AMBULATORY;

        @com.fasterxml.jackson.annotation.JsonCreator
        public static ProviderType fromValue(String value) {
            return ProviderType.valueOf(value.toUpperCase());
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String toValue() {
            return name().toLowerCase();
        }
    }

    @jakarta.persistence.Converter(autoApply = false)
    public static class ProviderTypeConverter
            implements jakarta.persistence.AttributeConverter<ProviderType, String> {

        @Override
        public String convertToDatabaseColumn(ProviderType attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public ProviderType convertToEntityAttribute(String dbData) {
            return dbData != null ? ProviderType.valueOf(dbData.toUpperCase()) : null;
        }
    }
}
