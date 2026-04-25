package com.Billing_System.repository;

import com.Billing_System.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    boolean existsByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime time);
}
