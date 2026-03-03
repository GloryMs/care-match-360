package com.carebillingservice.controller;

import com.carebillingservice.dto.*;
import com.carebillingservice.service.DemoSubscriptionService;
import com.carecommon.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Demo / simulation controller for the subscription & payment flow.
 *
 * Base path: /api/v1/demo/subscriptions
 *
 * These endpoints simulate the complete billing cycle WITHOUT calling
 * the real Stripe API. All records are persisted to the real DB tables so
 * the UI can display realistic subscription, invoice, and payment data.
 *
 * The existing production endpoints (/api/v1/subscriptions, /api/v1/invoices)
 * are NOT affected.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  ENDPOINT SUMMARY (for front-end team)                                  │
 * │                                                                         │
 * │  POST   /api/v1/demo/subscriptions                                      │
 * │     Simulate full subscribe + pay in one shot.                          │
 * │     Body: { providerId, tier, simulatePaymentFailure? }                 │
 * │     Returns: { subscription, invoice, payment, simulationSummary }      │
 * │                                                                         │
 * │  PUT    /api/v1/demo/subscriptions/{subscriptionId}/upgrade             │
 * │     Simulate tier upgrade or downgrade.                                 │
 * │     Body: { newTier }                                                   │
 * │     Returns: { subscription, invoice, payment, simulationSummary }      │
 * │                                                                         │
 * │  DELETE /api/v1/demo/subscriptions/{subscriptionId}                     │
 * │     Simulate subscription cancellation.                                 │
 * │     Returns: updated SubscriptionResponse                               │
 * │                                                                         │
 * │  GET    /api/v1/demo/subscriptions/provider/{providerId}                │
 * │     Get the current subscription for a provider.                        │
 * │     Returns: SubscriptionResponse                                       │
 * │                                                                         │
 * │  GET    /api/v1/demo/subscriptions/{subscriptionId}/invoices            │
 * │     List all invoices for a subscription (paginated).                   │
 * │     Params: page (default 0), size (default 10)                         │
 * │     Returns: List<InvoiceResponse>                                      │
 * │                                                                         │
 * │  GET    /api/v1/demo/subscriptions/{subscriptionId}/payments            │
 * │     List all payment history records for a subscription (paginated).    │
 * │     Params: page (default 0), size (default 10)                         │
 * │     Returns: List<PaymentHistoryResponse>                               │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/demo/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class DemoSubscriptionController {

    private final DemoSubscriptionService demoSubscriptionService;

    // ── 1. Simulate subscribe + pay ───────────────────────────────────────────

    /**
     * POST /api/v1/demo/subscriptions
     *
     * Simulates the complete subscription purchase:
     *   1. Creates a Subscription (status = ACTIVE, fake Stripe IDs).
     *   2. Generates an Invoice (PAID or PENDING based on simulatePaymentFailure).
     *   3. Records a PaymentHistory entry (SUCCEEDED or FAILED).
     *   4. Records SubscriptionHistory.
     *   5. Publishes Kafka events.
     *
     * Request body:
     * {
     *   "providerId": "uuid",
     *   "tier": "BASIC | PRO | PREMIUM",
     *   "simulatePaymentFailure": false    // optional, default false
     * }
     *
     * 201 Created on success.
     * 400 if the provider already has an active subscription.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DemoSubscriptionResult>> simulateSubscription(
            @Valid @RequestBody DemoSubscriptionRequest request) {

        log.info("[DEMO] POST /demo/subscriptions — providerId={}, tier={}",
                request.getProviderId(), request.getTier());

        DemoSubscriptionResult result = demoSubscriptionService.simulateSubscription(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, result.getSimulationSummary()));
    }

    // ── 2. Simulate upgrade / downgrade ──────────────────────────────────────

    /**
     * PUT /api/v1/demo/subscriptions/{subscriptionId}/upgrade
     *
     * Simulates changing the subscription tier (upgrade or downgrade).
     * Creates a prorated invoice and payment record for the new tier.
     *
     * Path variable: subscriptionId (UUID)
     * Request body: { "newTier": "BASIC | PRO | PREMIUM" }
     *
     * 200 OK on success.
     */
    @PutMapping("/{subscriptionId}/upgrade")
    public ResponseEntity<ApiResponse<DemoSubscriptionResult>> simulateUpgrade(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody DemoUpgradeRequest request) {

        log.info("[DEMO] PUT /demo/subscriptions/{}/upgrade — newTier={}", subscriptionId, request.getNewTier());

        DemoSubscriptionResult result = demoSubscriptionService.simulateUpgrade(subscriptionId, request);
        return ResponseEntity.ok(ApiResponse.success(result, result.getSimulationSummary()));
    }

    // ── 3. Simulate cancellation ──────────────────────────────────────────────

    /**
     * DELETE /api/v1/demo/subscriptions/{subscriptionId}
     *
     * Simulates cancelling the subscription (status → CANCELLED).
     * Records history and publishes Kafka event.
     *
     * 200 OK on success.
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> simulateCancel(
            @PathVariable UUID subscriptionId) {

        log.info("[DEMO] DELETE /demo/subscriptions/{}", subscriptionId);

        SubscriptionResponse response = demoSubscriptionService.simulateCancel(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription cancelled (demo simulation)"));
    }

    // ── 4. Get subscription by provider ──────────────────────────────────────

    /**
     * GET /api/v1/demo/subscriptions/provider/{providerId}
     *
     * Returns the current subscription and plan info for the given provider.
     * Works for both demo-created and real subscriptions.
     *
     * 200 OK, 404 if not found.
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getByProvider(
            @PathVariable UUID providerId) {

        SubscriptionResponse response = demoSubscriptionService.getByProvider(providerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── 5. List invoices for a subscription ──────────────────────────────────

    /**
     * GET /api/v1/demo/subscriptions/{subscriptionId}/invoices
     *
     * Returns paginated list of invoices for the subscription, newest first.
     *
     * Query params:
     *   page (default 0)
     *   size (default 10, max 50)
     */
    @GetMapping("/{subscriptionId}/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices(
            @PathVariable UUID subscriptionId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);
        List<InvoiceResponse> invoices = demoSubscriptionService.getInvoices(subscriptionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    // ── 6. List payment history for a subscription ────────────────────────────

    /**
     * GET /api/v1/demo/subscriptions/{subscriptionId}/payments
     *
     * Returns paginated payment history for the subscription, newest first.
     *
     * Query params:
     *   page (default 0)
     *   size (default 10, max 50)
     */
    @GetMapping("/{subscriptionId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPayments(
            @PathVariable UUID subscriptionId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);
        List<PaymentHistoryResponse> payments = demoSubscriptionService.getPayments(subscriptionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
