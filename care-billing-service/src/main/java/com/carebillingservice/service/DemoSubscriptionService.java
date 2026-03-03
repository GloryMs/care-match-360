package com.carebillingservice.service;

import com.carebillingservice.config.SubscriptionPlanConfig;
import com.carebillingservice.dto.*;
import com.carebillingservice.kafka.BillingEventProducer;
import com.carebillingservice.model.Invoice;
import com.carebillingservice.model.PaymentHistory;
import com.carebillingservice.model.Subscription;
import com.carebillingservice.model.SubscriptionHistory;
import com.carebillingservice.repository.InvoiceRepository;
import com.carebillingservice.repository.PaymentHistoryRepository;
import com.carebillingservice.repository.SubscriptionHistoryRepository;
import com.carebillingservice.repository.SubscriptionRepository;
import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.kafkaEvents.PaymentFailedEvent;
import com.carecommon.kafkaEvents.PaymentSucceededEvent;
import com.carecommon.kafkaEvents.SubscriptionCreatedEvent;
import com.carecommon.kafkaEvents.SubscriptionUpdatedEvent;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Demo/simulation service that replicates the full subscription + payment flow
 * WITHOUT calling the real Stripe API.
 *
 * All Stripe IDs are generated locally using a "demo_" prefix so they are
 * easily distinguishable from real credentials.
 *
 * All data is persisted to the same DB tables (subscriptions, invoices,
 * payment_history, subscription_history) so the front-end sees realistic records.
 * Kafka events are published exactly as the real service does.
 *
 * The existing SubscriptionService / InvoiceService are NOT modified.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemoSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SubscriptionPlanConfig subscriptionPlanConfig;
    private final BillingEventProducer billingEventProducer;

    private static final Random RANDOM = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the full "choose plan → pay" flow in a single call.
     * Creates: Subscription → Invoice (PAID or PENDING) → PaymentHistory → SubscriptionHistory.
     * Publishes Kafka events.
     *
     * If the provider already has a subscription, a {@link ValidationException} is thrown.
     */
    @Transactional
    public DemoSubscriptionResult simulateSubscription(DemoSubscriptionRequest request) {
        log.info("[DEMO] Simulating subscription: providerId={}, tier={}, failureMode={}",
                request.getProviderId(), request.getTier(), request.isSimulatePaymentFailure());

        if (subscriptionRepository.findByProviderId(request.getProviderId()).isPresent()) {
            throw new ValidationException(
                    "A subscription already exists for this provider. " +
                    "Cancel it first or use the upgrade endpoint.");
        }

        Subscription.SubscriptionTier tier = parseTier(request.getTier());
        SubscriptionPlanInfo planInfo = subscriptionPlanConfig.getPlanInfo(tier);

        // 1. Create subscription
        Subscription subscription = buildDemoSubscription(request.getProviderId(), tier);
        subscription = subscriptionRepository.save(subscription);
        log.info("[DEMO] Subscription saved: id={}", subscription.getId());

        // 2. Create invoice
        boolean paymentSucceeds = !request.isSimulatePaymentFailure();
        Invoice invoice = buildDemoInvoice(subscription, planInfo, paymentSucceeds);
        invoice = invoiceRepository.save(invoice);
        log.info("[DEMO] Invoice saved: id={}, number={}, status={}", invoice.getId(), invoice.getInvoiceNumber(), invoice.getStatus());

        // 3. Create payment history
        PaymentHistory payment = buildDemoPayment(subscription, invoice, paymentSucceeds);
        payment = paymentHistoryRepository.save(payment);
        log.info("[DEMO] Payment saved: id={}, status={}", payment.getId(), payment.getStatus());

        // 4. Record subscription history
        saveSubscriptionHistory(subscription.getId(), null, tier.name(),
                null, subscription.getStatus().name(), request.getProviderId(), "Demo subscription created");

        // 5. Publish Kafka events
        publishSubscriptionCreatedEvent(subscription);
        if (paymentSucceeds) {
            publishPaymentSucceededEvent(subscription, invoice, payment);
        } else {
            publishPaymentFailedEvent(subscription, invoice, payment);
        }

        // 6. Build response
        String summary = paymentSucceeds
                ? String.format("Subscription ACTIVE. Invoice %s paid (EUR %.2f). Payment SUCCEEDED.",
                        invoice.getInvoiceNumber(), planInfo.getPrice())
                : String.format("Subscription created but payment FAILED. Invoice %s is PENDING.",
                        invoice.getInvoiceNumber());

        return DemoSubscriptionResult.builder()
                .subscription(toSubscriptionResponse(subscription))
                .invoice(toInvoiceResponse(invoice))
                .payment(toPaymentResponse(payment))
                .simulationSummary(summary)
                .build();
    }

    /**
     * Simulates upgrading or downgrading the subscription tier.
     * Creates a prorated invoice and payment record for the new tier.
     */
    @Transactional
    public DemoSubscriptionResult simulateUpgrade(UUID subscriptionId, DemoUpgradeRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        Subscription.SubscriptionTier newTier = parseTier(request.getNewTier());
        if (subscription.getTier() == newTier) {
            throw new ValidationException("Subscription is already on the " + newTier + " tier.");
        }

        log.info("[DEMO] Simulating tier change: subscriptionId={}, {} → {}",
                subscriptionId, subscription.getTier(), newTier);

        String oldTier = subscription.getTier().name();
        String oldStatus = subscription.getStatus().name();

        // Update subscription
        subscription.setTier(newTier);
        subscription.setStripePriceId(fakePriceId(newTier));
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        subscription = subscriptionRepository.save(subscription);

        // Create prorated invoice
        SubscriptionPlanInfo planInfo = subscriptionPlanConfig.getPlanInfo(newTier);
        Invoice invoice = buildDemoInvoice(subscription, planInfo, true);
        invoice = invoiceRepository.save(invoice);

        // Create payment record
        PaymentHistory payment = buildDemoPayment(subscription, invoice, true);
        payment = paymentHistoryRepository.save(payment);

        // Record history
        saveSubscriptionHistory(subscriptionId, oldTier, newTier.name(),
                oldStatus, subscription.getStatus().name(), subscription.getProviderId(), "Demo tier change");

        // Publish events
        SubscriptionUpdatedEvent event = SubscriptionUpdatedEvent.builder()
                .eventType("subscription.updated")
                .subscriptionId(subscriptionId)
                .providerId(subscription.getProviderId())
                .oldTier(oldTier)
                .newTier(newTier.name())
                .oldStatus(oldStatus)
                .newStatus(subscription.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();
        billingEventProducer.sendSubscriptionUpdatedEvent(event);
        publishPaymentSucceededEvent(subscription, invoice, payment);

        String summary = String.format("Tier changed %s → %s. New invoice %s paid (EUR %.2f).",
                oldTier, newTier.name(), invoice.getInvoiceNumber(), planInfo.getPrice());

        return DemoSubscriptionResult.builder()
                .subscription(toSubscriptionResponse(subscription))
                .invoice(toInvoiceResponse(invoice))
                .payment(toPaymentResponse(payment))
                .simulationSummary(summary)
                .build();
    }

    /**
     * Simulates cancelling the subscription.
     * Sets status to CANCELLED and records history + Kafka event.
     */
    @Transactional
    public SubscriptionResponse simulateCancel(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        log.info("[DEMO] Simulating cancellation: subscriptionId={}", subscriptionId);

        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription = subscriptionRepository.save(subscription);

        saveSubscriptionHistory(subscriptionId, null, null,
                oldStatus, Subscription.SubscriptionStatus.CANCELLED.name(),
                subscription.getProviderId(), "Demo subscription cancelled");

        SubscriptionUpdatedEvent event = SubscriptionUpdatedEvent.builder()
                .eventType("subscription.updated")
                .subscriptionId(subscriptionId)
                .providerId(subscription.getProviderId())
                .oldTier(subscription.getTier().name())
                .newTier(subscription.getTier().name())
                .oldStatus(oldStatus)
                .newStatus(Subscription.SubscriptionStatus.CANCELLED.name())
                .timestamp(LocalDateTime.now())
                .build();
        billingEventProducer.sendSubscriptionUpdatedEvent(event);

        return toSubscriptionResponse(subscription);
    }

    /**
     * Returns the subscription + plan info for a given provider.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getByProvider(UUID providerId) {
        Subscription subscription = subscriptionRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "providerId", providerId));
        return toSubscriptionResponse(subscription);
    }

    /**
     * Returns all invoices for a subscription (delegating to the shared invoice repository).
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(UUID subscriptionId, int page, int size) {
        ModelMapper mapper = new ModelMapper();
        return invoiceRepository
                .findBySubscriptionIdOrderByIssuedAtDesc(subscriptionId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(i -> mapper.map(i, InvoiceResponse.class))
                .collect(Collectors.toList());
    }

    /**
     * Returns all payment history records for a subscription.
     */
    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPayments(UUID subscriptionId, int page, int size) {
        ModelMapper mapper = new ModelMapper();
        return paymentHistoryRepository
                .findBySubscriptionIdOrderByProcessedAtDesc(subscriptionId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(p -> mapper.map(p, PaymentHistoryResponse.class))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Subscription buildDemoSubscription(UUID providerId, Subscription.SubscriptionTier tier) {
        return Subscription.builder()
                .providerId(providerId)
                .tier(tier)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .stripeCustomerId("cus_demo_" + randomAlphanumeric(10))
                .stripeSubscriptionId("sub_demo_" + randomAlphanumeric(10))
                .stripePriceId(fakePriceId(tier))
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .trialEnd(null)
                .build();
    }

    private Invoice buildDemoInvoice(Subscription subscription, SubscriptionPlanInfo planInfo,
                                     boolean paymentSucceeds) {
        LocalDateTime now = LocalDateTime.now();
        Invoice.InvoiceStatus status = paymentSucceeds
                ? Invoice.InvoiceStatus.PAID
                : Invoice.InvoiceStatus.PENDING;

        String piId = "pi_demo_" + randomAlphanumeric(10);

        return Invoice.builder()
                .subscriptionId(subscription.getId())
                .invoiceNumber(generateInvoiceNumber())
                .amount(planInfo.getPrice())
                .currency(planInfo.getCurrency() != null ? planInfo.getCurrency() : "EUR")
                .status(status)
                .stripeInvoiceId("in_demo_" + randomAlphanumeric(10))
                .stripePaymentIntentId(piId)
                .issuedAt(now)
                .dueAt(now.plusDays(7))
                .paidAt(paymentSucceeds ? now : null)
                .build();
    }

    private PaymentHistory buildDemoPayment(Subscription subscription, Invoice invoice,
                                            boolean succeeds) {
        PaymentHistory.PaymentStatus status = succeeds
                ? PaymentHistory.PaymentStatus.SUCCEEDED
                : PaymentHistory.PaymentStatus.FAILED;

        return PaymentHistory.builder()
                .invoiceId(invoice.getId())
                .subscriptionId(subscription.getId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .paymentMethod("DEMO_CARD_" + randomDigits(4))
                .stripePaymentIntentId(invoice.getStripePaymentIntentId())
                .stripeChargeId("ch_demo_" + randomAlphanumeric(10))
                .status(status)
                .failureReason(succeeds ? null : "Demo simulated payment failure (insufficient_funds)")
                .processedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kafka helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void publishSubscriptionCreatedEvent(Subscription subscription) {
        try {
            SubscriptionCreatedEvent event = SubscriptionCreatedEvent.builder()
                    .eventType("subscription.created")
                    .subscriptionId(subscription.getId())
                    .providerId(subscription.getProviderId())
                    .tier(subscription.getTier().name())
                    .timestamp(LocalDateTime.now())
                    .build();
            billingEventProducer.sendSubscriptionCreatedEvent(event);
        } catch (Exception e) {
            log.warn("[DEMO] Failed to publish subscription created event", e);
        }
    }

    private void publishPaymentSucceededEvent(Subscription subscription, Invoice invoice,
                                               PaymentHistory payment) {
        try {
            PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                    .eventType("payment.succeeded")
                    .subscriptionId(subscription.getId())
                    .invoiceId(invoice.getId())
                    .providerId(subscription.getProviderId())
                    .amount(invoice.getAmount())
                    .currency(invoice.getCurrency())
                    .timestamp(LocalDateTime.now())
                    .build();
            billingEventProducer.sendPaymentSucceededEvent(event);
        } catch (Exception e) {
            log.warn("[DEMO] Failed to publish payment succeeded event", e);
        }
    }

    private void publishPaymentFailedEvent(Subscription subscription, Invoice invoice,
                                            PaymentHistory payment) {
        try {
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .eventType("payment.failed")
                    .subscriptionId(subscription.getId())
                    .invoiceId(invoice.getId())
                    .providerId(subscription.getProviderId())
                    .amount(invoice.getAmount())
                    .currency(invoice.getCurrency())
                    .failureReason(payment.getFailureReason())
                    .timestamp(LocalDateTime.now())
                    .build();
            billingEventProducer.sendPaymentFailedEvent(event);
        } catch (Exception e) {
            log.warn("[DEMO] Failed to publish payment failed event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapping helpers
    // ─────────────────────────────────────────────────────────────────────────

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        ModelMapper mapper = new ModelMapper();
        SubscriptionResponse response = mapper.map(subscription, SubscriptionResponse.class);
        response.setPlanInfo(subscriptionPlanConfig.getPlanInfo(subscription.getTier()));
        return response;
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new ModelMapper().map(invoice, InvoiceResponse.class);
    }

    private PaymentHistoryResponse toPaymentResponse(PaymentHistory payment) {
        return new ModelMapper().map(payment, PaymentHistoryResponse.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Subscription.SubscriptionTier parseTier(String tier) {
        try {
            return Subscription.SubscriptionTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid tier '" + tier + "'. Allowed values: BASIC, PRO, PREMIUM");
        }
    }

    private String fakePriceId(Subscription.SubscriptionTier tier) {
        return switch (tier) {
            case BASIC   -> "price_demo_basic_monthly";
            case PRO     -> "price_demo_pro_monthly";
            case PREMIUM -> "price_demo_premium_monthly";
        };
    }

    private String generateInvoiceNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "INV-" + datePart + "-" + randomAlphanumeric(6).toUpperCase();
    }

    private void saveSubscriptionHistory(UUID subscriptionId, String oldTier, String newTier,
                                         String oldStatus, String newStatus,
                                         UUID changedBy, String notes) {
        subscriptionHistoryRepository.save(SubscriptionHistory.builder()
                .subscriptionId(subscriptionId)
                .oldTier(oldTier)
                .newTier(newTier)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes("[DEMO] " + notes)
                .build());
    }

    private String randomAlphanumeric(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
