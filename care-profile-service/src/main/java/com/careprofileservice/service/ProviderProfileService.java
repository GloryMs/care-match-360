package com.careprofileservice.service;

import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.kafkaEvents.ProfileCreatedEvent;
import com.carecommon.kafkaEvents.ProfileUpdatedEvent;
import com.careprofileservice.dto.*;
import com.careprofileservice.kafka.ProfileEventProducer;
import com.careprofileservice.mapper.ProviderProfileMapper;
import com.careprofileservice.model.ProviderProfile;
import com.careprofileservice.model.SearchHistory;
import com.careprofileservice.repository.ProviderProfileRepository;
import com.careprofileservice.repository.SearchHistoryRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderProfileService {

    private final ProviderProfileRepository providerProfileRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ProviderProfileMapper providerProfileMapper;
    private final ProfileEventProducer profileEventProducer;

    @Transactional
    public void createBasicProfile(UUID userId, String email, ProviderProfile.ProviderType providerType) {
        if (providerProfileRepository.existsByUserId(userId)) {
            log.warn("Provider profile already exists for userId={}, skipping creation", userId);
            return;
        }

        ProviderProfile profile = ProviderProfile.builder()
                .userId(userId)
                .email(email)
                .providerType(providerType)
                .build();
        profile = providerProfileRepository.save(profile);
        log.info("Basic provider profile created: userId={}, profileId={}", userId, profile.getId());

        ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .eventType("profile.created")
                .profileId(profile.getId())
                .profileType("provider")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        profileEventProducer.sendProfileCreatedEvent(event);
    }

    @Transactional
    public ProviderProfileResponse createProfile(UUID userId, CreateProviderProfileRequest request) {
        // Check if profile already exists
        if (providerProfileRepository.existsByUserId(userId)) {
            throw new ValidationException("Provider profile already exists for this user");
        }

        // Create profile
        ProviderProfile profile = providerProfileMapper.toEntity(request);
        profile.setUserId(userId);

        profile = providerProfileRepository.save(profile);
        log.info("Provider profile created: userId={}, profileId={}", userId, profile.getId());

        // Publish event
        ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .eventType("profile.created")
                .profileId(profile.getId())
                .profileType("provider")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        profileEventProducer.sendProfileCreatedEvent(event);

        return providerProfileMapper.toResponse(profile);
    }

    /**
     * Returns all visible provider profiles (isVisible = true).
     * Used by the matching engine when a patient profile is created or updated.
     */
    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> getAllActiveProviders() {
        // findByIsVisibleTrue(Pageable) exists; we use the List overload below.
        // If that overload doesn't exist yet, add it to ProviderProfileRepository:
        //   List<ProviderProfile> findByIsVisibleTrue();
        return providerProfileRepository.findByIsVisibleTrue()
                .stream()
                .map(providerProfileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "providerProfiles", key = "#userId")
    public ProviderProfileResponse updateProfile(UUID userId, UpdateProviderProfileRequest request) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile", "userId", userId));

        // Track changes for event
        Map<String, Object> changes = trackChanges(profile, request);

        // Update profile
        providerProfileMapper.updateEntity(request, profile);
        profile = providerProfileRepository.save(profile);

        log.info("Provider profile updated: userId={}, profileId={}", userId, profile.getId());

        // Publish event if there were changes
        if (!changes.isEmpty()) {
            ProfileUpdatedEvent event = ProfileUpdatedEvent.builder()
                    .eventType("profile.updated")
                    .profileId(profile.getId())
                    .profileType("provider")
                    .userId(userId)
                    .changes(changes)
                    .timestamp(LocalDateTime.now())
                    .build();

            profileEventProducer.sendProfileUpdatedEvent(event);
        }

        return providerProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "providerProfiles", key = "#userId")
    public ProviderProfileResponse getProfileByUserId(UUID userId) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile", "userId", userId));

        return providerProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "providerProfiles", key = "#profileId")
    public ProviderProfileResponse getProfileById(UUID profileId) {

        log.info("Get provide profile by id: {}", profileId);
        log.info("No cashing yet :) => Redis");

        ProviderProfile profile;
        ProviderProfileResponse profileResponse = new ProviderProfileResponse();
        try{
            ModelMapper mapper = new ModelMapper();
            profile = providerProfileRepository.findById(profileId)
                    .orElseThrow(() -> new ResourceNotFoundException("Provider profile", "id", profileId));
            log.info("Profile found = > Id: {}", profile.getId());
            profileResponse =  mapper.map(profile, ProviderProfileResponse.class);
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
    public ProviderSearchResponse searchProviders(UUID userId, ProviderSearchRequest searchRequest) {
        log.info("Searching providers with criteria: {}", searchRequest);

        List<ProviderProfile> providers;

        // If location-based search
        if (searchRequest.getLatitude() != null &&
                searchRequest.getLongitude() != null &&
                searchRequest.getRadiusKm() != null) {

            double radiusMeters = searchRequest.getRadiusKm() * 1000.0;
            String providerType = searchRequest.getProviderType() != null ?
                    searchRequest.getProviderType().toUpperCase() : null;

            providers = providerProfileRepository.findProvidersNearLocation(
                    providerType,
                    searchRequest.getLatitude(),
                    searchRequest.getLongitude(),
                    radiusMeters
            );
        } else {
            // General search
            Pageable pageable = PageRequest.of(
                    searchRequest.getPage(),
                    searchRequest.getSize()
            );

            Page<ProviderProfile> page = providerProfileRepository.findByIsVisibleTrue(pageable);
            providers = page.getContent();
        }

        // Apply additional filters
        providers = applyFilters(providers, searchRequest);

        // Convert to response
        List<ProviderProfileResponse> providerResponses = providers.stream()
                .map(providerProfileMapper::toResponse)
                .collect(Collectors.toList());

        // Save search history
        saveSearchHistory(userId, searchRequest, providers.size());

        int totalResults = providers.size();
        int totalPages = (int) Math.ceil((double) totalResults / searchRequest.getSize());

        return ProviderSearchResponse.builder()
                .providers(providerResponses)
                .totalResults(totalResults)
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .totalPages(totalPages)
                .build();
    }

    @Transactional
    @CacheEvict(value = "providerProfiles", key = "#userId")
    public void deleteProfile(UUID userId) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile", "userId", userId));

        providerProfileRepository.delete(profile);
        log.info("Provider profile deleted: userId={}, profileId={}", userId, profile.getId());
    }

    private List<ProviderProfile> applyFilters(List<ProviderProfile> providers, ProviderSearchRequest request) {
        return providers.stream()
                .filter(p -> filterBySpecializations(p, request.getSpecializations()))
                .filter(p -> filterByCapacity(p, request.getMinCapacity()))
                .collect(Collectors.toList());
    }

    private boolean filterBySpecializations(ProviderProfile provider, List<String> requiredSpecializations) {
        if (requiredSpecializations == null || requiredSpecializations.isEmpty()) {
            return true;
        }

        if (provider.getSpecializations() == null || provider.getSpecializations().isEmpty()) {
            return false;
        }

        List<String> providerSpecs = provider.getSpecializations().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        return requiredSpecializations.stream()
                .map(String::toLowerCase)
                .anyMatch(providerSpecs::contains);
    }

    private boolean filterByCapacity(ProviderProfile provider, Integer minCapacity) {
        if (minCapacity == null) {
            return true;
        }

        return provider.getCapacity() != null && provider.getCapacity() >= minCapacity;
    }

    private void saveSearchHistory(UUID userId, ProviderSearchRequest request, int resultsCount) {
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("providerType", request.getProviderType());
            criteria.put("region", request.getRegion());
            criteria.put("radiusKm", request.getRadiusKm());
            criteria.put("careLevel", request.getCareLevel());
            criteria.put("specializations", request.getSpecializations());

            SearchHistory searchHistory = SearchHistory.builder()
                    .userId(userId)
                    .searchCriteria(criteria)
                    .resultsCount(resultsCount)
                    .searchedAt(LocalDateTime.now())
                    .build();

            searchHistoryRepository.save(searchHistory);
        } catch (Exception e) {
            log.warn("Failed to save search history for user: {}", userId, e);
        }
    }

    private Map<String, Object> trackChanges(ProviderProfile profile, UpdateProviderProfileRequest request) {
        Map<String, Object> changes = new HashMap<>();

        if (request.getAvailableRooms() != null && !request.getAvailableRooms().equals(profile.getAvailableRooms())) {
            changes.put("availableRooms", Map.of("old", profile.getAvailableRooms(), "new", request.getAvailableRooms()));
        }

        if (request.getSpecializations() != null && !request.getSpecializations().equals(profile.getSpecializations())) {
            changes.put("specializations", Map.of("old", profile.getSpecializations(), "new", request.getSpecializations()));
        }

        return changes;
    }
}
