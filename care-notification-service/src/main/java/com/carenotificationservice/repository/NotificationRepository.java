package com.carenotificationservice.repository;

import com.carenotificationservice.model.Notification;
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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByRecipientId(@Param("recipientId") UUID recipientId);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3")
    List<Notification> findFailedNotificationsForRetry();

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
