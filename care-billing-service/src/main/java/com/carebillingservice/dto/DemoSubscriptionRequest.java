package com.carebillingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request body for the demo/simulation subscription endpoint.
 * No real Stripe credentials are needed — the service generates fake IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoSubscriptionRequest {

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    @NotBlank(message = "Subscription tier is required (BASIC | PRO | PREMIUM)")
    private String tier;

    /**
     * When true, the payment simulation will record a FAILED payment
     * and leave the invoice as PENDING — useful for demoing error states.
     */
    @Builder.Default
    private boolean simulatePaymentFailure = false;
}
