package com.careprofileservice.service;

import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.kafkaEvents.ProfileCreatedEvent;
import com.carecommon.kafkaEvents.ProfileUpdatedEvent;
import com.careprofileservice.dto.CreatePatientProfileRequest;
import com.careprofileservice.dto.PatientProfileResponse;
import com.careprofileservice.dto.UpdatePatientProfileRequest;
import com.careprofileservice.kafka.ProfileEventProducer;
import com.careprofileservice.mapper.PatientProfileMapper;
import com.careprofileservice.model.PatientProfile;
import com.careprofileservice.repository.PatientProfileRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final PatientProfileMapper patientProfileMapper;
    private final ProfileEventProducer profileEventProducer;

    @Transactional
    public void createBasicProfile(UUID userId, String email) {
        if (patientProfileRepository.existsByUserId(userId)) {
            log.warn("Patient profile already exists for userId={}, skipping creation", userId);
            return;
        }

        PatientProfile profile = PatientProfile.builder()
                .userId(userId)
                .email(email)
                .build();
        profile = patientProfileRepository.save(profile);
        log.info("Basic patient profile created: userId={}, profileId={}", userId, profile.getId());

        ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .eventType("profile.created")
                .profileId(profile.getId())
                .profileType("patient")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        profileEventProducer.sendProfileCreatedEvent(event);
    }

    @Transactional
    public PatientProfileResponse createProfile(UUID userId, CreatePatientProfileRequest request) {
        // Check if profile already exists
        if (patientProfileRepository.existsByUserId(userId)) {
            throw new ValidationException("Patient profile already exists for this user");
        }

        // Create profile
        PatientProfile profile = patientProfileMapper.toEntity(request);
        profile.setUserId(userId);

        profile = patientProfileRepository.save(profile);
        log.info("Patient profile created: userId={}, profileId={}", userId, profile.getId());

        // Publish event
        ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .eventType("profile.created")
                .profileId(profile.getId())
                .profileType("patient")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        profileEventProducer.sendProfileCreatedEvent(event);

        return patientProfileMapper.toResponse(profile);
    }

    /**
     * Returns all patient profiles where the patient has given data-sharing consent.
     * Used by the matching engine to fan out score calculation when a provider
     * profile is created or updated (FR-MATCH-01).
     */
    @Transactional(readOnly = true)
    public List<PatientProfileResponse> getAllActivePatients() {
        return patientProfileRepository.findByConsentGivenTrue()
                .stream()
                .map(patientProfileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "patientProfiles", key = "#userId")
    public PatientProfileResponse updateProfile(UUID userId, UpdatePatientProfileRequest request) {
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", "userId", userId));

        // Track changes for event
        Map<String, Object> changes = trackChanges(profile, request);

        // Update profile
        patientProfileMapper.updateEntity(request, profile);
        profile = patientProfileRepository.save(profile);

        log.info("Patient profile updated: userId={}, profileId={}", userId, profile.getId());

        // Publish event if there were changes
        if (!changes.isEmpty()) {
            ProfileUpdatedEvent event = ProfileUpdatedEvent.builder()
                    .eventType("profile.updated")
                    .profileId(profile.getId())
                    .profileType("patient")
                    .userId(userId)
                    .changes(changes)
                    .timestamp(LocalDateTime.now())
                    .build();

            profileEventProducer.sendProfileUpdatedEvent(event);
        }

        return patientProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "patientProfiles", key = "#userId")
    public PatientProfileResponse getProfileByUserId(UUID userId) {
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", "userId", userId));

        return patientProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "patientProfiles", key = "#profileId")
    public PatientProfileResponse getProfileById(UUID profileId) {
        log.info("Get profile by id: {}", profileId);
        log.info("No cashing yet :) => Redis");
        PatientProfile profile;
        PatientProfileResponse profileResponse = new PatientProfileResponse();
        try{
            ModelMapper mapper = new ModelMapper();
            profile = patientProfileRepository.findById(profileId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile", "id", profileId));
            log.info("Profile found = > Id: {}", profile.getId());
            profileResponse =  mapper.map(profile, PatientProfileResponse.class);
            profileResponse.setLatitude(profile.getLocation().getY());
            profileResponse.setLongitude(profile.getLocation().getX());
            log.info("Get profile latitude: {}", profileResponse.getLatitude());
            log.info("Get profile longitude: {}", profileResponse.getLongitude());
            log.info("Profile mapped = > Id from mapping: {}", profileResponse.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("And now return the profile");
        return profileResponse;
    }

    @Transactional
    @CacheEvict(value = "patientProfiles", key = "#userId")
    public void deleteProfile(UUID userId) {
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", "userId", userId));

        patientProfileRepository.delete(profile);
        log.info("Patient profile deleted: userId={}, profileId={}", userId, profile.getId());
    }

    private Map<String, Object> trackChanges(PatientProfile profile, UpdatePatientProfileRequest request) {
        Map<String, Object> changes = new HashMap<>();

        if (request.getCareLevel() != null && !request.getCareLevel().equals(profile.getCareLevel())) {
            changes.put("careLevel", Map.of("old", profile.getCareLevel(), "new", request.getCareLevel()));
        }

        if (request.getCareType() != null && !request.getCareType().equals(profile.getCareType())) {
            changes.put("careType", Map.of("old", profile.getCareType(), "new", request.getCareType()));
        }

        if (request.getRegion() != null && !request.getRegion().equals(profile.getRegion())) {
            changes.put("region", Map.of("old", profile.getRegion(), "new", request.getRegion()));
        }

        return changes;
    }
}
