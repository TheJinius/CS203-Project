package com.ubs.tariffapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable()) // Disabled - using custom CorsFilter instead
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/health").permitAll() //allow healthcheck!
                .requestMatchers("/api/oauth2/exchange-token").permitAll() // Allow token exchange (must be before /api/**)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/h2-console/**").permitAll() //TODO: REMOVE THIS IN PROD
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api/swagger-resources/**").permitAll() // Allow Swagger UI
                .requestMatchers("/swagger-oauth2-redirect").permitAll() // Allow custom OAuth2 redirect
                .requestMatchers("/oauth2-redirect.html").permitAll() // Allow custom OAuth2 redirect page
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.disable())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
