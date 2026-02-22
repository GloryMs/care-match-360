package com.carebillingservice.config;

import com.carebillingservice.dto.SubscriptionPlanInfo;
import com.carebillingservice.model.Subscription;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Getter
public class SubscriptionPlanConfig {

    private final Map<Subscription.SubscriptionTier, SubscriptionPlanInfo> plans = new HashMap<>();

    public SubscriptionPlanConfig(
            @Value("${subscription.plans.basic.name}") String basicName,
            @Value("${subscription.plans.basic.price}") BigDecimal basicPrice,
            @Value("${subscription.plans.basic.currency}") String basicCurrency,
            @Value("${subscription.plans.basic.interval}") String basicInterval,
            @Value("${subscription.plans.basic.features}") String basicFeatures,

            @Value("${subscription.plans.pro.name}") String proName,
            @Value("${subscription.plans.pro.price}") BigDecimal proPrice,
            @Value("${subscription.plans.pro.currency}") String proCurrency,
            @Value("${subscription.plans.pro.interval}") String proInterval,
            @Value("${subscription.plans.pro.features}") String proFeatures,

            @Value("${subscription.plans.premium.name}") String premiumName,
            @Value("${subscription.plans.premium.price}") BigDecimal premiumPrice,
            @Value("${subscription.plans.premium.currency}") String premiumCurrency,
            @Value("${subscription.plans.premium.interval}") String premiumInterval,
            @Value("${subscription.plans.premium.features}") String premiumFeatures
    ) {
        plans.put(Subscription.SubscriptionTier.BASIC, SubscriptionPlanInfo.builder()
                .name(basicName)
                .price(basicPrice)
                .currency(basicCurrency)
                .interval(basicInterval)
                .features(Arrays.asList(basicFeatures.split(",")))
                .build());

        plans.put(Subscription.SubscriptionTier.PRO, SubscriptionPlanInfo.builder()
                .name(proName)
                .price(proPrice)
                .currency(proCurrency)
                .interval(proInterval)
                .features(Arrays.asList(proFeatures.split(",")))
                .build());

        plans.put(Subscription.SubscriptionTier.PREMIUM, SubscriptionPlanInfo.builder()
                .name(premiumName)
                .price(premiumPrice)
                .currency(premiumCurrency)
                .interval(premiumInterval)
                .features(Arrays.asList(premiumFeatures.split(",")))
                .build());
    }

    public SubscriptionPlanInfo getPlanInfo(Subscription.SubscriptionTier tier) {
        return plans.get(tier);
    }
}
