package com.carenotificationservice.feign;

import com.carecommon.dto.ApiResponse;
import com.carenotificationservice.dto.ProfileSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "care-profile-service", path = "/api/v1")
public interface ProfileServiceClient {

    @GetMapping("/patients/{profileId}")
    ApiResponse<ProfileSummaryDTO> getPatientProfile(@PathVariable("profileId") UUID profileId);

    @GetMapping("/providers/{profileId}")
    ApiResponse<ProfileSummaryDTO> getProviderProfile(@PathVariable("profileId") UUID profileId);
}
