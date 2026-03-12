package com.careprofileservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Feign client scanning for care-profile-service.
 *
 * Scans com.careprofileservice.feign, which currently contains:
 *   - {@link com.careprofileservice.feign.BillingServiceClient}
 *
 * Follows the same pattern as care-match-service's FeignConfig.
 */
@Configuration
@EnableFeignClients(basePackages = "com.careprofileservice.feign")
public class FeignConfig {
    // Feign client configuration — beans are auto-registered by Spring Cloud OpenFeign
}