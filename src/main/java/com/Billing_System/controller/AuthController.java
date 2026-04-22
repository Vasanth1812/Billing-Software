package com.Billing_System.controller;

import com.Billing_System.dto.*;
import com.Billing_System.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     *
     * Two ways to login:
     * Way 1: { "email": "john@shop.com", "password": "secret123" }
     * Way 2: { "userId": "EMP001", "password": "secret123" }
     *
     * Returns JWT token + user details on success.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/forgot-password
     *
     * Send email to receive reset token.
     * Body: { "email": "john@shop.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * POST /api/auth/reset-password
     *
     * Reset password using the token received via email.
     * Body: { "token": "uuid-reset-token", "newPassword": "newSecret456" }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
