package com.careprofileservice.repository;

import com.careprofileservice.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

    Optional<PatientProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query(value = "SELECT * FROM care_profiles.patient_profiles p WHERE :careType = ANY(p.care_type)", nativeQuery = true)
    List<PatientProfile> findByCareType(@Param("careType") String careType);

    List<PatientProfile> findByCareLevel(Integer careLevel);

    List<PatientProfile> findByConsentGivenTrue();
}
