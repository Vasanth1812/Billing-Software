package com.Billing_System.config;

import com.Billing_System.repository.BlacklistedTokenRepository;
import com.Billing_System.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Filter — runs BEFORE every request.
 *
 * Reads the "Authorization: Bearer <token>" header,
 * validates the JWT, and sets the authenticated user
 * into Spring Security's context.
 *
 * If no token or invalid token → request continues as anonymous
 * → Spring Security will block it if the endpoint requires auth.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token? Let it through — SecurityConfig will decide if endpoint needs auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            if (blacklistedTokenRepository.existsByToken(token)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtUtil.isTokenValid(token)) {
                Claims claims = jwtUtil.parseToken(token);
                String role = claims.get("role", String.class);
                String userId = claims.getSubject(); // UUID

                // Set authenticated user into SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                           // principal (user UUID)
                                null,                                             // credentials (not needed)
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Invalid token — don't set auth, request continues as anonymous
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
