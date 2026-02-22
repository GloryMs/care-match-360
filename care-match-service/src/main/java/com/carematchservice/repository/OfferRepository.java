package com.carematchservice.repository;


import com.carematchservice.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    Page<Offer> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

    List<Offer> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    Page<Offer> findByProviderIdOrderByCreatedAtDesc(UUID providerId, Pageable pageable);

    List<Offer> findByPatientIdAndStatus(UUID patientId, Offer.OfferStatus status);

    List<Offer> findByProviderIdAndStatus(UUID providerId, Offer.OfferStatus status);

    @Query("SELECT o FROM Offer o WHERE o.status = :status AND o.expiresAt < :now")
    List<Offer> findExpiredOffers(@Param("status") Offer.OfferStatus status, @Param("now") LocalDateTime now);

    long countByPatientIdAndStatus(UUID patientId, Offer.OfferStatus status);

    long countByProviderIdAndStatus(UUID providerId, Offer.OfferStatus status);
}
