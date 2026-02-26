package com.carenotificationservice.kafka;

import com.carecommon.kafkaEvents.*;
import com.carenotificationservice.service.AnalyticsService;
import com.carenotificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @KafkaListener(
            topics = "${kafka.topics.profile-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProfileCreated(
            @Payload ProfileCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing profile created event: profileId={}, profileType={}",
                event.getProfileId(), event.getProfileType());

        // Log event
        analyticsService.logEvent(event.getProfileId(), "profile.created", null);
    }

    @KafkaListener(
            topics = "${kafka.topics.match-calculated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMatchCalculated(
            @Payload MatchCalculatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing match calculated event: matchId={}, score={}",
                event.getMatchId(), event.getScore());

        // Send notification to provider about high match
        if (event.getScore().doubleValue() >= 70) {
            notificationService.sendMatchNotification(event);
        }

        // Log event
        analyticsService.logEvent(event.getProviderId(), "match.calculated", null);
    }

    @KafkaListener(
            topics = "${kafka.topics.offer-sent}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOfferSent(
            @Payload OfferSentEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing offer sent event: offerId={}", event.getOfferId());

        // Send notification to patient
        notificationService.sendOfferReceivedNotification(event);

        // Log event
        analyticsService.logEvent(event.getPatientId(), "offer.received", null);
    }

    @KafkaListener(
            topics = "${kafka.topics.offer-accepted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOfferAccepted(
            @Payload OfferAcceptedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing offer accepted event: offerId={}", event.getOfferId());

        // Send notification to provider
        notificationService.sendOfferAcceptedNotification(event);

        // Log event
        analyticsService.logEvent(event.getProviderId(), "offer.accepted", null);
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-succeeded}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentSucceeded(
            @Payload PaymentSucceededEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing payment succeeded event: invoiceId={}", event.getInvoiceId());

        // Send payment confirmation notification
        notificationService.sendPaymentSuccessNotification(event);

        // Log event
        analyticsService.logEvent(event.getProviderId(), "payment.succeeded", null);
    }

    @KafkaListener(
            topics = "${kafka.topics.subscription-expired}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubscriptionExpired(
            @Payload SubscriptionExpiredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Processing subscription expired event: subscriptionId={}", event.getSubscriptionId());

        // Send subscription expiration warning
        notificationService.sendSubscriptionExpiredNotification(event);

        // Log event
        analyticsService.logEvent(event.getProviderId(), "subscription.expired", null);
    }
}