package com.carematchservice.feign;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "care-profile-service", path = "/api/v1")
public interface ProfileServiceClient {

    @GetMapping("/patients/{profileId}")
    ApiResponse<PatientProfileDTO> getPatientProfile(@PathVariable("profileId") UUID profileId);

    @GetMapping("/providers/{profileId}")
    ApiResponse<ProviderProfileDTO> getProviderProfile(@PathVariable("profileId") UUID profileId);
}
