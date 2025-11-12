package com.ubs.tariffapp.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()//allow cors preflight
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
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * Configures JWT authentication converter to extract authorities from Cognito groups.
     * Cognito stores user groups in the "cognito:groups" claim.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Extracts authorities from the "cognito:groups" claim in the JWT token.
     * Maps each group name to a GrantedAuthority without adding any prefix.
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            System.out.println("====== JWT TOKEN DEBUG ======");
            System.out.println("All claims: " + jwt.getClaims());
            
            // Extract groups from "cognito:groups" claim
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            System.out.println("Cognito groups found: " + groups);
            
            if (groups == null || groups.isEmpty()) {
                System.out.println("WARNING: No groups found in token!");
                return List.of();
            }
            
            // Convert each group to a GrantedAuthority without prefix
            List<GrantedAuthority> authorities = groups.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            System.out.println("Granted authorities: " + authorities);
            System.out.println("============================");
            
            return authorities;
        };
    }
}
