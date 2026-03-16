package com.carenotificationservice.service;

import com.carenotificationservice.dto.NotificationPreferencesDTO;
import com.carenotificationservice.model.NotificationPreferences;
import com.carenotificationservice.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository repository;

    /**
     * Get notification preferences for a profile.
     * Returns default preferences (all true) if none exist yet.
     */
    @Transactional(readOnly = true)
    public NotificationPreferencesDTO getPreferences(UUID profileId) {
        return repository.findByProfileId(profileId)
                .map(this::toDTO)
                .orElseGet(() -> {
                    log.debug("No preferences found for profileId={}, returning defaults", profileId);
                    return NotificationPreferencesDTO.builder()
                            .emailEnabled(true)
                            .inAppEnabled(true)
                            .matchAlerts(true)
                            .offerAlerts(true)
                            .systemAlerts(true)
                            .build();
                });
    }

    /**
     * Create or update notification preferences for a profile.
     * Uses upsert logic — creates the row on first call, updates on subsequent calls.
     */
    @Transactional
    public NotificationPreferencesDTO updatePreferences(UUID profileId, NotificationPreferencesDTO dto) {
        NotificationPreferences prefs = repository.findByProfileId(profileId)
                .orElseGet(() -> {
                    log.info("Creating notification preferences for profileId={}", profileId);
                    return NotificationPreferences.builder()
                            .profileId(profileId)
                            .build();
                });

        if (dto.getEmailEnabled()  != null) prefs.setEmailEnabled(dto.getEmailEnabled());
        if (dto.getInAppEnabled()  != null) prefs.setInAppEnabled(dto.getInAppEnabled());
        if (dto.getMatchAlerts()   != null) prefs.setMatchAlerts(dto.getMatchAlerts());
        if (dto.getOfferAlerts()   != null) prefs.setOfferAlerts(dto.getOfferAlerts());
        if (dto.getSystemAlerts()  != null) prefs.setSystemAlerts(dto.getSystemAlerts());

        NotificationPreferences saved = repository.save(prefs);
        log.info("Saved notification preferences for profileId={}: email={}, inApp={}, match={}, offer={}, system={}",
                profileId, saved.getEmailEnabled(), saved.getInAppEnabled(),
                saved.getMatchAlerts(), saved.getOfferAlerts(), saved.getSystemAlerts());

        return toDTO(saved);
    }

    /**
     * Check if a specific notification channel is enabled for a profile.
     * Used by Kafka consumers before dispatching notifications.
     */
    public boolean isEmailEnabled(UUID profileId) {
        return repository.findByProfileId(profileId)
                .map(NotificationPreferences::getEmailEnabled)
                .orElse(true); // default: enabled
    }

    public boolean isInAppEnabled(UUID profileId) {
        return repository.findByProfileId(profileId)
                .map(NotificationPreferences::getInAppEnabled)
                .orElse(true);
    }

    public boolean isAlertTypeEnabled(UUID profileId, String alertType) {
        return repository.findByProfileId(profileId)
                .map(prefs -> switch (alertType.toUpperCase()) {
                    case "MATCH"  -> prefs.getMatchAlerts();
                    case "OFFER"  -> prefs.getOfferAlerts();
                    case "SYSTEM" -> prefs.getSystemAlerts();
                    default       -> true;
                })
                .orElse(true);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────
    private NotificationPreferencesDTO toDTO(NotificationPreferences entity) {
        return NotificationPreferencesDTO.builder()
                .emailEnabled(entity.getEmailEnabled())
                .inAppEnabled(entity.getInAppEnabled())
                .matchAlerts(entity.getMatchAlerts())
                .offerAlerts(entity.getOfferAlerts())
                .systemAlerts(entity.getSystemAlerts())
                .build();
    }
}