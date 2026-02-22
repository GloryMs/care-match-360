package com.carebillingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    @NotBlank(message = "Subscription tier is required")
    private String tier; // BASIC, PRO, PREMIUM

    private String paymentMethodId; // Stripe payment method ID
}
