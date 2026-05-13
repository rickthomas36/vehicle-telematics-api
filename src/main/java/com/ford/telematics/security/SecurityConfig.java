package com.ford.telematics.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 *
 * For local development, all endpoints are open so you can test freely.
 *
 * PHASE 2 (your next milestone):
 * Add JWT authentication here — require a Bearer token on all
 * /api/v1/** routes. This is how Ford's real APIs work.
 *
 * The JWT dependency is already in pom.xml waiting for you.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST APIs (stateless, no cookies)
            .csrf(AbstractHttpConfigurer::disable)

            // Allow H2 console frames (dev only)
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

            // Allow all requests for now — add JWT filter here in Phase 2
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/vehicles/health").permitAll()
                .anyRequest().permitAll()  // ← Change to .authenticated() when adding JWT
            );

        return http.build();
    }
}
