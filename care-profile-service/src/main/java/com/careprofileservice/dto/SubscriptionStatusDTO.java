package com.careprofileservice.dto;

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

    /** True when the provider's subscription is ACTIVE or TRIALING. */
    private Boolean isActive;

    /** The raw status string (e.g. "ACTIVE", "TRIALING", "CANCELLED"). */
    private String status;

    /** The subscription tier (e.g. "BASIC", "PRO", "PREMIUM"). Optional. */
    private String tier;
}