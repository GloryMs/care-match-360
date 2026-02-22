package com.carebillingservice.repository;

import com.carebillingservice.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByProviderId(UUID providerId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.currentPeriodEnd < :now")
    List<Subscription> findExpiredSubscriptions(
            @Param("status") Subscription.SubscriptionStatus status,
            @Param("now") LocalDateTime now
    );

    long countByStatus(Subscription.SubscriptionStatus status);

    long countByTier(Subscription.SubscriptionTier tier);
}
