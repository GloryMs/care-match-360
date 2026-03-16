package com.carenotificationservice.repository;

import com.carenotificationservice.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {

    Optional<NotificationPreferences> findByProfileId(UUID profileId);

    boolean existsByProfileId(UUID profileId);
}