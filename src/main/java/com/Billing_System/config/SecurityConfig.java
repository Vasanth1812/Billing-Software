package com.Billing_System.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration:
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ /api/auth/**          → OPEN (login, forgot-password, reset)│
 * │ /api/**               → JWT REQUIRED (all other APIs)       │
 * │ Everything else       → BLOCKED                             │
 * └──────────────────────────────────────────────────────────────┘
 *
 * - BCrypt for password hashing
 * - Stateless sessions (JWT, no cookies)
 * - CSRF disabled (REST API)
 * - CORS configured for frontend
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC — no token needed ──────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()

                // ── ADMIN only ────────────────────────────────────────────────
                // Only ADMIN can manage users and bulk import
                .requestMatchers(HttpMethod.POST,   "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // Only ADMIN can manage suppliers (traceability anchor)
                .requestMatchers(HttpMethod.POST,   "/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/suppliers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**").hasRole("ADMIN")

                // ── ADMIN + MANAGER ───────────────────────────────────────────
                // Products: ADMIN and MANAGER can create/update/delete/import
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "MANAGER")

                // Categories: ADMIN and MANAGER can create/update/delete
                .requestMatchers(HttpMethod.POST,   "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")

                // Purchases: ADMIN and MANAGER only
                .requestMatchers(HttpMethod.POST,   "/api/purchases/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/purchases/**").hasAnyRole("ADMIN", "MANAGER")

                // ── ALL ROLES (ADMIN + MANAGER + CASHIER) ────────────────────
                // Cashier can process sales
                .requestMatchers("/api/sales/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")

                // Cashier can look up products (for POS / barcode scan)
                .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers(HttpMethod.GET, "/api/categories/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers(HttpMethod.GET, "/api/suppliers/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers(HttpMethod.GET, "/api/purchases/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")

                // Reports: ADMIN and MANAGER only
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration — allows frontend (any origin for dev).
     * In production, replace "*" with your actual frontend URL.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));          // all origins for dev
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));                  // allow Authorization header
        config.setAllowCredentials(true);                        // allow cookies/auth headers
        config.setMaxAge(3600L);                                 // cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
