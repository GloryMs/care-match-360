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
    
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    void deleteByUser(User user);
}