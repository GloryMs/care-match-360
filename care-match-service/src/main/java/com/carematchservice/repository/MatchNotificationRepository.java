package com.carematchservice.repository;


import com.carematchservice.model.MatchNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchNotificationRepository extends JpaRepository<MatchNotification, UUID> {

    Optional<MatchNotification> findByMatchId(UUID matchId);

    boolean existsByMatchIdAndNotificationSentTrue(UUID matchId);
}
