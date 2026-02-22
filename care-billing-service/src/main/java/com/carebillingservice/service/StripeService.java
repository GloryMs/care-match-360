package com.carebillingservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public Customer createCustomer(String email, String name) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();

        Customer customer = Customer.create(params);
        log.info("Stripe customer created: customerId={}, email={}", customer.getId(), email);
        return customer;
    }

    public PaymentMethod attachPaymentMethod(String paymentMethodId, String customerId) throws StripeException {
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build();

        paymentMethod.attach(params);
        log.info("Payment method attached: paymentMethodId={}, customerId={}", paymentMethodId, customerId);
        return paymentMethod;
    }

    public void setDefaultPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
        CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                        CustomerUpdateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod(paymentMethodId)
                                .build()
                )
                .build();

        Customer customer = Customer.retrieve(customerId);
        customer.update(params);
        log.info("Default payment method set: customerId={}, paymentMethodId={}", customerId, paymentMethodId);
    }

    public com.stripe.model.Subscription createSubscription(
            String customerId,
            String priceId,
            int trialDays) throws StripeException {

        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                        SubscriptionCreateParams.Item.builder()
                                .setPrice(priceId)
                                .build()
                );

        if (trialDays > 0) {
            paramsBuilder.setTrialPeriodDays((long) trialDays);
        }

        com.stripe.model.Subscription subscription = com.stripe.model.Subscription.create(paramsBuilder.build());
        log.info("Stripe subscription created: subscriptionId={}, customerId={}",
                subscription.getId(), customerId);
        return subscription;
    }

    public com.stripe.model.Subscription updateSubscription(
            String subscriptionId,
            String newPriceId) throws StripeException {

        com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);

        SubscriptionItem subscriptionItem = subscription.getItems().getData().get(0);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                        SubscriptionUpdateParams.Item.builder()
                                .setId(subscriptionItem.getId())
                                .setPrice(newPriceId)
                                .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();

        subscription = subscription.update(params);
        log.info("Stripe subscription updated: subscriptionId={}, newPriceId={}", subscriptionId, newPriceId);
        return subscription;
    }

    public com.stripe.model.Subscription cancelSubscription(String subscriptionId) throws StripeException {
        com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);

        SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                .setProrate(true)
                .build();

        subscription = subscription.cancel(params);
        log.info("Stripe subscription cancelled: subscriptionId={}", subscriptionId);
        return subscription;
    }

    public Invoice retrieveInvoice(String invoiceId) throws StripeException {
        return Invoice.retrieve(invoiceId);
    }

    public Price createPrice(String productId, BigDecimal amount, String currency, String interval)
            throws StripeException {

        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(productId)
                .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setCurrency(currency)
                .setRecurring(
                        PriceCreateParams.Recurring.builder()
                                .setInterval(PriceCreateParams.Recurring.Interval.valueOf(interval.toUpperCase()))
                                .build()
                )
                .build();

        return Price.create(params);
    }

    public Event constructWebhookEvent(String payload, String signature, String webhookSecret)
            throws com.stripe.exception.SignatureVerificationException {

        return Webhook.constructEvent(payload, signature, webhookSecret);
    }
}
