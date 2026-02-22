package com.carenotificationservice.repository;

import com.carenotificationservice.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByTemplateName(String templateName);

    List<NotificationTemplate> findByChannel(String channel);

    List<NotificationTemplate> findByIsActiveTrue();
}
