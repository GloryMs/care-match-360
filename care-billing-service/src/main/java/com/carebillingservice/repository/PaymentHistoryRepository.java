package com.carebillingservice.repository;

import com.carebillingservice.model.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    List<PaymentHistory> findByInvoiceIdOrderByProcessedAtDesc(UUID invoiceId);

    List<PaymentHistory> findBySubscriptionIdOrderByProcessedAtDesc(UUID subscriptionId);

    Page<PaymentHistory> findBySubscriptionIdOrderByProcessedAtDesc(UUID subscriptionId, Pageable pageable);

    Optional<PaymentHistory> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<PaymentHistory> findByStatus(PaymentHistory.PaymentStatus status);
}
