package com.carenotificationservice.feign;

import com.carecommon.dto.ApiResponse;
import com.carenotificationservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "care-identity-service", path = "/api/v1/users")
public interface IdentityServiceClient {

    @GetMapping("/{userId}")
    ApiResponse<UserDTO> getUserById(@PathVariable("userId") UUID userId);
}
