package com.carebillingservice.kafka;

import com.carebillingservice.kafka.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.subscription-created}")
    private String subscriptionCreatedTopic;

    @Value("${kafka.topics.subscription-updated}")
    private String subscriptionUpdatedTopic;

    @Value("${kafka.topics.subscription-expired}")
    private String subscriptionExpiredTopic;

    @Value("${kafka.topics.payment-succeeded}")
    private String paymentSucceededTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    public void sendSubscriptionCreatedEvent(SubscriptionCreatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(subscriptionCreatedTopic, event.getSubscriptionId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Subscription created event sent: subscriptionId={}", event.getSubscriptionId());
            } else {
                log.error("Failed to send subscription created event: subscriptionId={}",
                        event.getSubscriptionId(), ex);
            }
        });
    }

    public void sendSubscriptionUpdatedEvent(SubscriptionUpdatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(subscriptionUpdatedTopic, event.getSubscriptionId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Subscription updated event sent: subscriptionId={}", event.getSubscriptionId());
            } else {
                log.error("Failed to send subscription updated event: subscriptionId={}",
                        event.getSubscriptionId(), ex);
            }
        });
    }

    public void sendSubscriptionExpiredEvent(SubscriptionExpiredEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(subscriptionExpiredTopic, event.getSubscriptionId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Subscription expired event sent: subscriptionId={}", event.getSubscriptionId());
            } else {
                log.error("Failed to send subscription expired event: subscriptionId={}",
                        event.getSubscriptionId(), ex);
            }
        });
    }

    public void sendPaymentSucceededEvent(PaymentSucceededEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(paymentSucceededTopic, event.getSubscriptionId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Payment succeeded event sent: subscriptionId={}, invoiceId={}",
                        event.getSubscriptionId(), event.getInvoiceId());
            } else {
                log.error("Failed to send payment succeeded event: subscriptionId={}",
                        event.getSubscriptionId(), ex);
            }
        });
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(paymentFailedTopic, event.getSubscriptionId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Payment failed event sent: subscriptionId={}, reason={}",
                        event.getSubscriptionId(), event.getFailureReason());
            } else {
                log.error("Failed to send payment failed event: subscriptionId={}",
                        event.getSubscriptionId(), ex);
            }
        });
    }
}