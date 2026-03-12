package com.careprofileservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Lightweight DTO returned by care-billing-service's subscription status endpoint.
 *
 * GET /api/v1/subscriptions/provider/{providerId}/status
 *
 * Only the {@code isActive} flag is needed by PatientSearchService to gate
 * access to the patient search directory.
 *
 * Mirrors com.carematchservice.dto.SubscriptionStatusDTO in care-match-service.
 */
@Data
public class SubscriptionStatusDTO {

    /**
     * True when the provider's subscription is ACTIVE or TRIALING.
     * Billing service serializes the primitive boolean field as "active" (Jackson strips "is" prefix
     * from isXxx() getters), so we must bind to that exact JSON key.
     */
    @JsonProperty("active")
    private Boolean isActive;

    /** The raw status string (e.g. "ACTIVE", "TRIALING", "CANCELLED"). */
    private String status;

    /** The subscription tier (e.g. "BASIC", "PRO", "PREMIUM"). Optional. */
    private String tier;
}