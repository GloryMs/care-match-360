package com.carematchservice.feign;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.SubscriptionStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client to query subscription status from care-billing-service.
 * Used by OfferService to gate offer creation/sending behind an active subscription.
 */
@FeignClient(name = "care-billing-service", path = "/api/v1")
public interface BillingServiceClient {

    /**
     * Returns minimal subscription status for a provider.
     * Backed by: GET /api/v1/subscriptions/provider/{providerId}/status
     *           (new endpoint added to care-billing-service)
     */
    @GetMapping("/subscriptions/provider/{providerId}/status")
    ApiResponse<SubscriptionStatusDTO> getProviderSubscriptionStatus(@PathVariable("providerId") UUID providerId);
}