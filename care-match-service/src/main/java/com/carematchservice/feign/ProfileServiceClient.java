package com.carematchservice.feign;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "care-profile-service", path = "/api/v1")
public interface ProfileServiceClient {

    @GetMapping("/patients/{profileId}")
    ApiResponse<PatientProfileDTO> getPatientProfile(@PathVariable("profileId") UUID profileId);

    @GetMapping("/providers/{profileId}")
    ApiResponse<ProviderProfileDTO> getProviderProfile(@PathVariable("profileId") UUID profileId);

    /**
     * Returns all active patient profiles (isActive/consentGiven = true).
     * Used by calculateMatchesForProvider to fan out across all patients.
     *
     * Backed by: GET /api/v1/patients/all  in care-profile-service
     *   → PatientProfileController.getAllActivePatients()
     *   → PatientProfileService.getAllActivePatients()
     *   → patientProfileRepository.findByConsentGivenTrue()
     */
    @GetMapping("/patients/all")
    ApiResponse<List<PatientProfileDTO>> getAllActivePatients();

    /**
     * Returns all active / visible provider profiles.
     * Used by calculateMatchesForPatient to fan out across all providers.
     *
     * Backed by: GET /api/v1/providers/all  in care-profile-service
     *   → ProviderProfileController.getAllActiveProviders()
     *   → ProviderProfileService.getAllActiveProviders()
     *   → providerProfileRepository.findByIsVisibleTrue()  ← already exists in your repo
     */
    @GetMapping("/providers/all")
    ApiResponse<List<ProviderProfileDTO>> getAllActiveProviders();
}
