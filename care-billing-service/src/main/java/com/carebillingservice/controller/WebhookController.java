package com.carebillingservice.controller;

import com.carebillingservice.service.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
//@Tag(name = "Webhooks", description = "Webhook endpoints for external integrations")
public class WebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    //@Operation(summary = "Handle Stripe webhooks")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook");

        try {
            stripeWebhookService.handleWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }
}
