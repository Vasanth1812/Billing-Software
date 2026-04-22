package com.Billing_System.repository;

import com.Billing_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Login option 1: find by email */
    Optional<User> findByEmailAndIsActiveTrue(String email);

    /** Login option 2: find by userId (e.g. EMP001) */
    Optional<User> findByUserIdAndIsActiveTrue(String userId);

    /** Forgot password: find by reset token */
    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByEmail(String email);

    List<User> findByIsActiveTrue();

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);
}
