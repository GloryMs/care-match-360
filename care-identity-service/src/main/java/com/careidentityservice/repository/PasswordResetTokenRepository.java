package com.careidentityservice.repository;

import com.careidentityservice.model.PasswordResetToken;
import com.careidentityservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    /** Lookup a code for a specific user — avoids collisions across different users. */
    Optional<PasswordResetToken> findByUserAndToken(User user, String token);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    void deleteByUser(User user);
}