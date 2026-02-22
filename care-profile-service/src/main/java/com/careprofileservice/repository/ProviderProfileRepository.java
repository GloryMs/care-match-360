package com.careprofileservice.repository;

import com.careprofileservice.model.ProviderProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {

    Optional<ProviderProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<ProviderProfile> findByProviderTypeAndIsVisibleTrue(ProviderProfile.ProviderType providerType);

    Page<ProviderProfile> findByIsVisibleTrue(Pageable pageable);

    @Query(value = "SELECT * FROM care_profiles.provider_profiles " +
            "WHERE provider_type = :providerType " +
            "AND is_visible = true " +
            "AND ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters) " +
            "ORDER BY ST_Distance(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)",
            nativeQuery = true)
    List<ProviderProfile> findProvidersNearLocation(
            @Param("providerType") String providerType,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );
}
