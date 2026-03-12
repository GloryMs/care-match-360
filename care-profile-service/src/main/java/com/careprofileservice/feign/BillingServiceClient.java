package com.careprofileservice.feign;

import com.carecommon.dto.ApiResponse;
import com.careprofileservice.dto.SubscriptionStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for care-billing-service.
 * Used by {@link com.careprofileservice.service.PatientSearchService} to gate
 * the patient search directory behind an active provider subscription.
 * Mirrors the identical client in care-match-service
 * (com.carematchservice.feign.BillingServiceClient).
 * com.careprofileservice.feign package.
 */
@FeignClient(name = "care-billing-service", path = "/api/v1")
public interface BillingServiceClient {

    /**
     * Lightweight subscription status check.
     * Returns {@code isActive = true} when the provider's subscription
     * status is ACTIVE or TRIALING.
     * Backed by: GET /api/v1/subscriptions/provider/{providerId}/status
     */
    @GetMapping("/subscriptions/provider/{providerId}/status")
    ApiResponse<SubscriptionStatusDTO> getProviderSubscriptionStatus(
            @PathVariable("providerId") UUID providerId);
}