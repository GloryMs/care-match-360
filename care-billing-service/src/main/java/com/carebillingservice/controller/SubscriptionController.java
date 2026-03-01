package com.carebillingservice.controller;

import com.carebillingservice.dto.CreateSubscriptionRequest;
import com.carebillingservice.dto.SubscriptionResponse;
import com.carebillingservice.dto.SubscriptionStatusDTO;
import com.carebillingservice.dto.UpdateSubscriptionRequest;
import com.carebillingservice.service.SubscriptionService;
import com.carecommon.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
//@Tag(name = "Subscriptions", description = "Subscription management endpoints")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    //@Operation(summary = "Create subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {

        log.debug("Create subscription request: {}", request);
        SubscriptionResponse subscription = subscriptionService.createSubscription(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(subscription, "Subscription created successfully"));
    }

    /**
     * GET /api/v1/subscriptions/provider/{providerId}/status
     * Lightweight subscription status check used by care-match-service
     * to gate offer creation/sending (Requirement 6).
     * Returns isActive=true when subscription status is ACTIVE or TRIALING.
     */
    @GetMapping("/provider/{providerId}/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusDTO>> getProviderSubscriptionStatus(
            @PathVariable UUID providerId) {

        SubscriptionStatusDTO status = subscriptionService.getProviderSubscriptionStatus(providerId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PutMapping("/{subscriptionId}")
    //@Operation(summary = "Update subscription (upgrade/downgrade)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {

        SubscriptionResponse subscription = subscriptionService.updateSubscription(subscriptionId, request);
        return ResponseEntity.ok(ApiResponse.success(subscription, "Subscription updated successfully"));
    }

    @DeleteMapping("/{subscriptionId}")
    //@Operation(summary = "Cancel subscription")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(@PathVariable UUID subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Subscription cancelled successfully"));
    }

    @GetMapping("/{subscriptionId}")
    //@Operation(summary = "Get subscription by ID")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @PathVariable UUID subscriptionId) {

        SubscriptionResponse subscription = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    @GetMapping("/provider/{providerId}")
    //@Operation(summary = "Get subscription by provider ID")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionByProviderId(
            @PathVariable UUID providerId) {

        SubscriptionResponse subscription = subscriptionService.getSubscriptionByProviderId(providerId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }
}
