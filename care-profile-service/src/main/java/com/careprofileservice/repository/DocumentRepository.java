package com.careprofileservice.repository;


import com.careprofileservice.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByProfileIdAndProfileType(UUID profileId, Document.ProfileType profileType);

    List<Document> findByProfileId(UUID profileId);

    void deleteByProfileId(UUID profileId);
}