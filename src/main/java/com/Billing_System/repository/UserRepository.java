package com.Billing_System.repository;

import com.Billing_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByUserIdAndIsActiveTrue(String userId);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByIsActiveTrue();

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);
}
