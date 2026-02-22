package com.carebillingservice.repository;

import com.carebillingservice.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

    List<Invoice> findBySubscriptionIdOrderByIssuedAtDesc(UUID subscriptionId);

    Page<Invoice> findBySubscriptionIdOrderByIssuedAtDesc(UUID subscriptionId, Pageable pageable);

    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    long countBySubscriptionIdAndStatus(UUID subscriptionId, Invoice.InvoiceStatus status);
}
