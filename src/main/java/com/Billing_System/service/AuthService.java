package com.Billing_System.service;

import com.Billing_System.dto.*;
import com.Billing_System.entity.BlacklistedToken;
import com.Billing_System.entity.SalesInvoice;
import com.Billing_System.entity.User;
import com.Billing_System.repository.BlacklistedTokenRepository;
import com.Billing_System.repository.SalesInvoiceRepository;
import com.Billing_System.repository.UserRepository;
import com.Billing_System.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;  // nullable — app works without mail config

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository userRepository,
                       BlacklistedTokenRepository blacklistedTokenRepository,
                       SalesInvoiceRepository salesInvoiceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @org.springframework.lang.Nullable JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
    }

    /**
     * LOGIN — supports two modes:
     * 1. email + password
     * 2. userId + password (e.g. "EMP001" + password)
     */
    public LoginResponseDTO login(LoginRequestDTO request) {

        // Determine which login mode
        User user;
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Mode 1: email + password
            user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        } else if (request.getUserId() != null && !request.getUserId().isBlank()) {
            // Mode 2: userId + password
            user = userRepository.findByUserIdAndIsActiveTrue(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user ID or password"));
        } else {
            throw new IllegalArgumentException("Either email or userId must be provided");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUserId(), user.getEmail(), user.getRole());

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * FORGOT PASSWORD — Step 1: User sends email → system sends reset token via email.
     * Token is valid for 30 minutes.
     */
    public Map<String, String> forgotPassword(ForgotPasswordDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with email: " + request.getEmail()));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is deactivated. Contact admin.");
        }

        // Generate a random reset token (UUID-based)
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        // Send email with reset token
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("Billing System — Password Reset");
                message.setText(
                        "Hello " + user.getName() + ",\n\n" +
                        "You requested a password reset. Use this token to reset your password:\n\n" +
                        "Reset Token: " + resetToken + "\n\n" +
                        "This token expires in 30 minutes.\n\n" +
                        "If you didn't request this, ignore this email.\n\n" +
                        "— Billing System"
                );
                mailSender.send(message);
                log.info("Reset email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send reset email to {}: {}", user.getEmail(), e.getMessage());
            }
        } else {
            log.warn("Mail not configured. Reset token for {}: {}", user.getEmail(), resetToken);
        }

        return Map.of("message", "If the email exists, a password reset link has been sent.");
    }

    /**
     * LOGOUT — record last logout time and return shift sales.
     * Shift window = lastLoginAt -> now.
     */
    public Map<String, Object> logout(String token) {
        if (token == null || token.isBlank() || !jwtUtil.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        blacklistedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (!blacklistedTokenRepository.existsByToken(token)) {
            blacklistedTokenRepository.save(
                    BlacklistedToken.builder()
                            .token(token)
                            .expiresAt(jwtUtil.getExpirationDateTime(token))
                            .build());
        }

        UUID userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        LocalDateTime logoutAt = LocalDateTime.now();
        LocalDateTime shiftStart = user.getLastLoginAt() != null ? user.getLastLoginAt() : logoutAt;

        List<SalesInvoice> shiftSales = salesInvoiceRepository.findByCreatedAtRange(shiftStart, logoutAt);

        java.math.BigDecimal shiftTotal = shiftSales.stream()
                .map(SalesInvoice::getGrandTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        user.setLastLogoutAt(logoutAt);
        userRepository.save(user);

        List<Map<String, Object>> sales = shiftSales.stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "invoiceNumber", s.getInvoiceNumber(),
                        "invoiceDate", s.getInvoiceDate(),
                        "createdAt", s.getCreatedAt(),
                        "customerName", s.getCustomerName() != null ? s.getCustomerName() : "Cash Customer",
                        "grandTotal", s.getGrandTotal() != null ? s.getGrandTotal() : java.math.BigDecimal.ZERO,
                        "paymentMode", s.getPaymentMode() != null ? s.getPaymentMode() : "",
                        "status", s.getStatus() != null ? s.getStatus() : ""))
                .toList();

        return Map.of(
                "message", "Logged out successfully",
                "userId", user.getId(),
                "shiftStart", shiftStart,
                "shiftEnd", logoutAt,
                "invoiceCount", sales.size(),
                "shiftTotal", shiftTotal,
                "sales", sales);
    }

    /**
     * RESET PASSWORD — Step 2: User sends token + new password.
     */
    public Map<String, String> resetPassword(ResetPasswordDTO request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        // Check expiry
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // Clear expired token
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Reset token has expired. Please request a new one.");
        }

        // Set new password (hashed)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successful for user: {}", user.getUserId());
        return Map.of("message", "Password has been reset successfully. You can now login with your new password.");
    }
}
