package com.careidentityservice.repository;

import com.careidentityservice.model.TwoFactorAuth;
import com.careidentityservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, UUID> {
    
    Optional<TwoFactorAuth> findByUser(User user);
    
    Optional<TwoFactorAuth> findByUserId(UUID userId);
    
    void deleteByUser(User user);
}