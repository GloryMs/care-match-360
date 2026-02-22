package com.carebillingservice.repository;

import com.carebillingservice.model.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, UUID> {

    List<SubscriptionHistory> findBySubscriptionIdOrderByChangedAtDesc(UUID subscriptionId);
}
