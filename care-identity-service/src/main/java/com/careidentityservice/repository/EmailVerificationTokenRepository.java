package com.careidentityservice.repository;

import com.careidentityservice.model.EmailVerificationToken;
import com.careidentityservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    
    /** Lookup a code for a specific user — avoids collisions across different users. */
    Optional<EmailVerificationToken> findByUserAndToken(User user, String token);

    List<EmailVerificationToken> findByUser(User user);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    void deleteByUser(User user);
}