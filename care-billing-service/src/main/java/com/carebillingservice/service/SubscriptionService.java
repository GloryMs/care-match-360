package com.carebillingservice.service;

import com.carebillingservice.config.SubscriptionPlanConfig;
import com.carebillingservice.dto.CreateSubscriptionRequest;
import com.carebillingservice.dto.SubscriptionPlanInfo;
import com.carebillingservice.dto.SubscriptionResponse;
import com.carebillingservice.dto.UpdateSubscriptionRequest;
import com.carebillingservice.kafka.BillingEventProducer;
import com.carebillingservice.kafka.events.SubscriptionCreatedEvent;
import com.carebillingservice.kafka.events.SubscriptionExpiredEvent;
import com.carebillingservice.kafka.events.SubscriptionUpdatedEvent;
import com.carebillingservice.model.Subscription;
import com.carebillingservice.model.SubscriptionHistory;
import com.carebillingservice.repository.SubscriptionHistoryRepository;
import com.carebillingservice.repository.SubscriptionRepository;
import com.carecommon.exception.ResourceNotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final StripeService stripeService;
    private final BillingEventProducer billingEventProducer;
    private final SubscriptionPlanConfig subscriptionPlanConfig;

    @Value("${app.subscription.trial-days}")
    private int trialDays;

    @Value("${app.subscription.grace-period-days}")
    private int gracePeriodDays;

    // Mock Stripe Price IDs - In production, these should be created in Stripe Dashboard
    private static final String BASIC_PRICE_ID = "price_basic_monthly";
    private static final String PRO_PRICE_ID = "price_pro_monthly";
    private static final String PREMIUM_PRICE_ID = "price_premium_monthly";

    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription: providerId={}, tier={}", request.getProviderId(), request.getTier());

        // Check if subscription already exists
        if (subscriptionRepository.findByProviderId(request.getProviderId()).isPresent()) {
            throw new ValidationException("Subscription already exists for this provider");
        }

        try {
            // Parse tier
            Subscription.SubscriptionTier tier = Subscription.SubscriptionTier.valueOf(request.getTier().toUpperCase());

            // Create Stripe customer
            Customer customer = stripeService.createCustomer(
                    "provider_" + request.getProviderId() + "@carematch360.com",
                    "Provider " + request.getProviderId()
            );

            // Attach payment method if provided
            if (request.getPaymentMethodId() != null) {
                PaymentMethod paymentMethod = stripeService.attachPaymentMethod(
                        request.getPaymentMethodId(),
                        customer.getId()
                );
                stripeService.setDefaultPaymentMethod(customer.getId(), paymentMethod.getId());
            }

            // Get price ID based on tier
            String priceId = getPriceIdForTier(tier);

            // Create Stripe subscription
            com.stripe.model.Subscription stripeSubscription = stripeService.createSubscription(
                    customer.getId(),
                    priceId,
                    trialDays
            );

            // Create local subscription
            Subscription subscription = Subscription.builder()
                    .providerId(request.getProviderId())
                    .tier(tier)
                    .status(trialDays > 0 ? Subscription.SubscriptionStatus.TRIALING : Subscription.SubscriptionStatus.ACTIVE)
                    .stripeCustomerId(customer.getId())
                    .stripeSubscriptionId(stripeSubscription.getId())
                    .stripePriceId(priceId)
                    .currentPeriodStart(convertToLocalDateTime(stripeSubscription.getCurrentPeriodStart()))
                    .currentPeriodEnd(convertToLocalDateTime(stripeSubscription.getCurrentPeriodEnd()))
                    .trialEnd(stripeSubscription.getTrialEnd() != null ?
                            convertToLocalDateTime(stripeSubscription.getTrialEnd()) : null)
                    .build();

            subscription = subscriptionRepository.save(subscription);
            log.info("Subscription created: subscriptionId={}", subscription.getId());

            // Record history
            recordSubscriptionHistory(subscription.getId(), null, tier.name(), null,
                    subscription.getStatus().name(), request.getProviderId(), "Subscription created");

            // Publish event
            SubscriptionCreatedEvent event = SubscriptionCreatedEvent.builder()
                    .eventType("subscription.created")
                    .subscriptionId(subscription.getId())
                    .providerId(request.getProviderId())
                    .tier(tier.name())
                    .timestamp(LocalDateTime.now())
                    .build();

            billingEventProducer.sendSubscriptionCreatedEvent(event);

            return enrichSubscriptionResponse(subscription);

        } catch (StripeException e) {
            log.error("Stripe error creating subscription: providerId={}", request.getProviderId(), e);
            throw new RuntimeException("Failed to create subscription: " + e.getMessage(), e);
        }
    }

    @Transactional
    public SubscriptionResponse updateSubscription(UUID subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        log.info("Updating subscription: subscriptionId={}, currentTier={}, newTier={}",
                subscriptionId, subscription.getTier(), request.getNewTier());

        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(request.getNewTier().toUpperCase());

        if (subscription.getTier() == newTier) {
            throw new ValidationException("Subscription is already on " + newTier + " tier");
        }

        try {
            String oldTierName = subscription.getTier().name();
            String newPriceId = getPriceIdForTier(newTier);

            // Update Stripe subscription
            com.stripe.model.Subscription stripeSubscription = stripeService.updateSubscription(
                    subscription.getStripeSubscriptionId(),
                    newPriceId
            );

            // Update local subscription
            subscription.setTier(newTier);
            subscription.setStripePriceId(newPriceId);
            subscription.setCurrentPeriodStart(convertToLocalDateTime(stripeSubscription.getCurrentPeriodStart()));
            subscription.setCurrentPeriodEnd(convertToLocalDateTime(stripeSubscription.getCurrentPeriodEnd()));

            subscription = subscriptionRepository.save(subscription);
            log.info("Subscription updated: subscriptionId={}", subscriptionId);

            // Record history
            recordSubscriptionHistory(subscriptionId, oldTierName, newTier.name(), null, null,
                    subscription.getProviderId(), "Subscription tier changed");

            // Publish event
            SubscriptionUpdatedEvent event = SubscriptionUpdatedEvent.builder()
                    .eventType("subscription.updated")
                    .subscriptionId(subscriptionId)
                    .providerId(subscription.getProviderId())
                    .oldTier(oldTierName)
                    .newTier(newTier.name())
                    .oldStatus(subscription.getStatus().name())
                    .newStatus(subscription.getStatus().name())
                    .timestamp(LocalDateTime.now())
                    .build();

            billingEventProducer.sendSubscriptionUpdatedEvent(event);

            return enrichSubscriptionResponse(subscription);

        } catch (StripeException e) {
            log.error("Stripe error updating subscription: subscriptionId={}", subscriptionId, e);
            throw new RuntimeException("Failed to update subscription: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        log.info("Cancelling subscription: subscriptionId={}", subscriptionId);

        try {
            // Cancel Stripe subscription
            stripeService.cancelSubscription(subscription.getStripeSubscriptionId());

            String oldStatus = subscription.getStatus().name();

            // Update local subscription
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);
            log.info("Subscription cancelled: subscriptionId={}", subscriptionId);

            // Record history
            recordSubscriptionHistory(subscriptionId, null, null, oldStatus,
                    Subscription.SubscriptionStatus.CANCELLED.name(), subscription.getProviderId(),
                    "Subscription cancelled");

            // Publish event
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

        } catch (StripeException e) {
            log.error("Stripe error cancelling subscription: subscriptionId={}", subscriptionId, e);
            throw new RuntimeException("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        return enrichSubscriptionResponse(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionByProviderId(UUID providerId) {
        Subscription subscription = subscriptionRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "providerId", providerId));

        return enrichSubscriptionResponse(subscription);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // Run daily at 3 AM
    public void handleExpiredSubscriptions() {
        log.info("Running scheduled subscription expiration check");

        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(
                Subscription.SubscriptionStatus.ACTIVE,
                now
        );

        for (Subscription subscription : expiredSubscriptions) {
            try {
                String oldStatus = subscription.getStatus().name();
                subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                subscriptionRepository.save(subscription);

                // Record history
                recordSubscriptionHistory(subscription.getId(), null, null, oldStatus,
                        Subscription.SubscriptionStatus.PAST_DUE.name(), null,
                        "Subscription expired - payment overdue");

                // Publish event
                SubscriptionExpiredEvent event = SubscriptionExpiredEvent.builder()
                        .eventType("subscription.expired")
                        .subscriptionId(subscription.getId())
                        .providerId(subscription.getProviderId())
                        .timestamp(LocalDateTime.now())
                        .build();

                billingEventProducer.sendSubscriptionExpiredEvent(event);

                log.info("Subscription marked as past due: subscriptionId={}", subscription.getId());

            } catch (Exception e) {
                log.error("Error handling expired subscription: subscriptionId={}",
                        subscription.getId(), e);
            }
        }

        log.info("Processed {} expired subscriptions", expiredSubscriptions.size());
    }

    private String getPriceIdForTier(Subscription.SubscriptionTier tier) {
        return switch (tier) {
            case BASIC -> BASIC_PRICE_ID;
            case PRO -> PRO_PRICE_ID;
            case PREMIUM -> PREMIUM_PRICE_ID;
        };
    }

    private LocalDateTime convertToLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private void recordSubscriptionHistory(UUID subscriptionId, String oldTier, String newTier,
                                           String oldStatus, String newStatus, UUID changedBy, String notes) {

        SubscriptionHistory history = SubscriptionHistory.builder()
                .subscriptionId(subscriptionId)
                .oldTier(oldTier)
                .newTier(newTier)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes(notes)
                .build();

        subscriptionHistoryRepository.save(history);
    }

    private SubscriptionResponse enrichSubscriptionResponse(Subscription subscription) {
        ModelMapper mapper = new ModelMapper();
        // SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        SubscriptionResponse response = new  SubscriptionResponse();;
        response = mapper.map(subscription, SubscriptionResponse.class);
        SubscriptionPlanInfo planInfo = subscriptionPlanConfig.getPlanInfo(subscription.getTier());
        response.setPlanInfo(planInfo);
        return response;
    }
}
