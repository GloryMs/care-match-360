package com.carebillingservice.service;

import com.carebillingservice.kafka.BillingEventProducer;
import com.carecommon.kafkaEvents.PaymentFailedEvent;
import com.carecommon.kafkaEvents.PaymentSucceededEvent;
import com.carebillingservice.model.Invoice;
import com.carebillingservice.model.PaymentHistory;
import com.carebillingservice.model.Subscription;
import com.carebillingservice.repository.InvoiceRepository;
import com.carebillingservice.repository.PaymentHistoryRepository;
import com.carebillingservice.repository.SubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final InvoiceService invoiceService;
    private final BillingEventProducer billingEventProducer;
    private final StripeService stripeService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void handleWebhookEvent(String payload, String signature) {
        try {
            Event event = stripeService.constructWebhookEvent(payload, signature, webhookSecret);

            log.info("Stripe webhook received: eventType={}, eventId={}",
                    event.getType(), event.getId());

            switch (event.getType()) {
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                default:
                    log.debug("Unhandled webhook event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Error handling Stripe webhook", e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject instanceof com.stripe.model.Invoice) {
            com.stripe.model.Invoice stripeInvoice = (com.stripe.model.Invoice) stripeObject;

            log.info("Processing invoice payment succeeded: stripeInvoiceId={}", stripeInvoice.getId());

            Optional<Invoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId());
            Invoice invoice;

            if (invoiceOpt.isEmpty()) {
                // Create invoice if it doesn't exist
                String stripeSubscriptionId = stripeInvoice.getSubscription();
                Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                        .orElseThrow(() -> new RuntimeException("Subscription not found for Stripe subscription: " + stripeSubscriptionId));

                invoice = invoiceService.createInvoice(subscription.getId(), stripeInvoice.getId());
            } else {
                invoice = invoiceOpt.get();
            }

            // Update invoice status
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setStripePaymentIntentId(stripeInvoice.getPaymentIntent());
            invoice.setPaidAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            // Create payment history
            PaymentHistory payment = PaymentHistory.builder()
                    .invoiceId(invoice.getId())
                    .subscriptionId(invoice.getSubscriptionId())
                    .amount(invoice.getAmount())
                    .currency(invoice.getCurrency())
                    .stripePaymentIntentId(stripeInvoice.getPaymentIntent())
                    .status(PaymentHistory.PaymentStatus.SUCCEEDED)
                    .build();

            paymentHistoryRepository.save(payment);

            // Get subscription for event
            Subscription subscription = subscriptionRepository.findById(invoice.getSubscriptionId())
                    .orElseThrow();

            // Publish event
            PaymentSucceededEvent paymentEvent = PaymentSucceededEvent.builder()
                    .eventType("payment.succeeded")
                    .subscriptionId(invoice.getSubscriptionId())
                    .invoiceId(invoice.getId())
                    .providerId(subscription.getProviderId())
                    .amount(invoice.getAmount())
                    .currency(invoice.getCurrency())
                    .timestamp(LocalDateTime.now())
                    .build();

            billingEventProducer.sendPaymentSucceededEvent(paymentEvent);

            log.info("Invoice payment succeeded processed: invoiceId={}", invoice.getId());
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject instanceof com.stripe.model.Invoice) {
            com.stripe.model.Invoice stripeInvoice = (com.stripe.model.Invoice) stripeObject;

            log.info("Processing invoice payment failed: stripeInvoiceId={}", stripeInvoice.getId());

            Optional<Invoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId());

            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();

                // Update invoice status
                invoice.setStatus(Invoice.InvoiceStatus.FAILED);
                invoiceRepository.save(invoice);

                // Create payment history
                PaymentHistory payment = PaymentHistory.builder()
                        .invoiceId(invoice.getId())
                        .subscriptionId(invoice.getSubscriptionId())
                        .amount(invoice.getAmount())
                        .currency(invoice.getCurrency())
                        .status(PaymentHistory.PaymentStatus.FAILED)
                        .failureReason(stripeInvoice.getLastFinalizationError() != null ?
                                stripeInvoice.getLastFinalizationError().getMessage() : "Payment failed")
                        .build();

                paymentHistoryRepository.save(payment);

                // Get subscription for event
                Subscription subscription = subscriptionRepository.findById(invoice.getSubscriptionId())
                        .orElseThrow();

                // Publish event
                PaymentFailedEvent paymentEvent = PaymentFailedEvent.builder()
                        .eventType("payment.failed")
                        .subscriptionId(invoice.getSubscriptionId())
                        .invoiceId(invoice.getId())
                        .providerId(subscription.getProviderId())
                        .amount(invoice.getAmount())
                        .currency(invoice.getCurrency())
                        .failureReason(payment.getFailureReason())
                        .timestamp(LocalDateTime.now())
                        .build();

                billingEventProducer.sendPaymentFailedEvent(paymentEvent);

                log.info("Invoice payment failed processed: invoiceId={}", invoice.getId());
            }
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        log.info("Processing subscription updated webhook");
        // Handle subscription updates from Stripe
        // Update local subscription status, period dates, etc.
    }

    private void handleSubscriptionDeleted(Event event) {
        log.info("Processing subscription deleted webhook");
        // Handle subscription cancellation from Stripe
        // Update local subscription status
    }
}
