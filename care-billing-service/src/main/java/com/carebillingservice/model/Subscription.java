package com.carebillingservice.model;

import com.carecommon.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", schema = "care_billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @Column(name = "provider_id", nullable = false, unique = true)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", unique = true, length = 255)
    private String stripeSubscriptionId;

    @Column(name = "stripe_price_id", length = 255)
    private String stripePriceId;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public enum SubscriptionTier {
        BASIC,
        PRO,
        PREMIUM
    }

    public enum SubscriptionStatus {
        ACTIVE,
        TRIALING,
        PAUSED,
        CANCELLED,
        PAST_DUE
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING;
    }

    public boolean canUpgrade() {
        return isActive() && tier != SubscriptionTier.PREMIUM;
    }

    public boolean canDowngrade() {
        return isActive() && tier != SubscriptionTier.BASIC;
    }
}
