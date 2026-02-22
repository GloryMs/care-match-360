package com.careprofileservice.model;


import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "search_history", schema = "care_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Type(JsonBinaryType.class)
    @Column(name = "search_criteria", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> searchCriteria;

    @Column(name = "results_count")
    private Integer resultsCount;

    @Column(name = "searched_at", nullable = false)
    @Builder.Default
    private LocalDateTime searchedAt = LocalDateTime.now();
}
