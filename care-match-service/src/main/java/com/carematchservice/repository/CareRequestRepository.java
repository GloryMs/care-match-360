package com.carematchservice.repository;

import com.carematchservice.model.CareRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareRequestRepository extends JpaRepository<CareRequest, UUID> {

    /** Check for an existing active (PENDING) request — prevents duplicates. */
    Optional<CareRequest> findByPatientIdAndProviderIdAndStatus(
            UUID patientId, UUID providerId, CareRequest.RequestStatus status);

    /** Patient view: all requests the patient has submitted. */
    Page<CareRequest> findByPatientId(UUID patientId, Pageable pageable);

    /** Patient view: filter by status. */
    Page<CareRequest> findByPatientIdAndStatus(
            UUID patientId, CareRequest.RequestStatus status, Pageable pageable);

    /** Provider inbox: all requests addressed to this provider. */
    Page<CareRequest> findByProviderId(UUID providerId, Pageable pageable);

    /** Provider inbox: filter by status. */
    Page<CareRequest> findByProviderIdAndStatus(
            UUID providerId, CareRequest.RequestStatus status, Pageable pageable);
}