package com.carematchservice.kafka;

import com.carematchservice.kafka.events.ProfileCreatedEvent;
import com.carematchservice.kafka.events.ProfileUpdatedEvent;
import com.carematchservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEventConsumer {

    private final MatchingService matchingService;

    @KafkaListener(topics = "${kafka.topics.profile-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProfileCreated(com.carematchservice.kafka.events.ProfileCreatedEvent event) {

        log.info("Received profile created event: profileId={}, profileType={}",
                event.getProfileId(), event.getProfileType());

        try {
            if ("patient".equalsIgnoreCase(event.getProfileType())) {
                // Calculate matches for new patient with all providers
                matchingService.calculateMatchesForPatient(event.getProfileId());
            } else if ("provider".equalsIgnoreCase(event.getProfileType())) {
                // Calculate matches for new provider with all patients
                matchingService.calculateMatchesForProvider(event.getProfileId());
            }
        } catch (Exception e) {
            log.error("Error processing profile created event: profileId={}",
                    event.getProfileId(), e);
            // In production, you might want to send to a dead-letter queue
        }
    }

    @KafkaListener(topics = "${kafka.topics.profile-updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProfileUpdated(com.carematchservice.kafka.events.ProfileUpdatedEvent event) {

        log.info("Received profile updated event: profileId={}, profileType={}",
                event.getProfileId(), event.getProfileType());

        try {
            // Check if significant fields changed that require recalculation
            if (shouldRecalculateMatches(event)) {
                if ("patient".equalsIgnoreCase(event.getProfileType())) {
                    matchingService.recalculateMatchesForPatient(event.getProfileId());
                } else if ("provider".equalsIgnoreCase(event.getProfileType())) {
                    matchingService.recalculateMatchesForProvider(event.getProfileId());
                }
            } else {
                log.debug("Profile update does not require match recalculation: profileId={}",
                        event.getProfileId());
            }
        } catch (Exception e) {
            log.error("Error processing profile updated event: profileId={}",
                    event.getProfileId(), e);
        }
    }

    private boolean shouldRecalculateMatches(com.carematchservice.kafka.events.ProfileUpdatedEvent event) {
        if (event.getChanges() == null || event.getChanges().isEmpty()) {
            return false;
        }

        // Define fields that trigger recalculation
        String[] significantFields = {
                "careLevel", "careType", "region", "location",
                "specializations", "medicalRequirements", "availableRooms"
        };

        for (String field : significantFields) {
            if (event.getChanges().containsKey(field)) {
                return true;
            }
        }

        return false;
    }
}
