package com.ubs.tariffapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("CognitoGroupConverter Tests")
class CognitoGroupConverterTest {

    private CognitoGroupConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CognitoGroupConverter();
    }

    @Test
    @DisplayName("Should convert Admins group to ROLE_Admins authority")
    void convert_WithAdminsGroup_ReturnsAdminAuthority() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of("Admins"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_Admins")
        );
    }

    @Test
    @DisplayName("Should convert Users group to ROLE_Users authority")
    void convert_WithUsersGroup_ReturnsUserAuthority() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of("Users"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_Users")
        );
    }

    @Test
    @DisplayName("Should convert both Admins and Users groups")
    void convert_WithBothGroups_ReturnsBothAuthorities() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of("Admins", "Users"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).hasSize(2);
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_Admins"),
            new SimpleGrantedAuthority("ROLE_Users")
        );
    }

    @Test
    @DisplayName("Should filter out unrecognized groups")
    void convert_WithUnrecognizedGroups_FiltersThemOut() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of("Admins", "SuperAdmins", "Users", "Guests"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).hasSize(2);
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_Admins"),
            new SimpleGrantedAuthority("ROLE_Users")
        );
    }

    @Test
    @DisplayName("Should return empty set when only unrecognized groups present")
    void convert_WithOnlyUnrecognizedGroups_ReturnsEmptySet() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of("SuperAdmins", "Guests", "Moderators"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when cognito:groups claim is missing")
    void convert_WithMissingClaim_ReturnsEmptySet() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-id-123"); // JWT requires at least one claim
        // No cognito:groups claim
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when cognito:groups claim is null")
    void convert_WithNullClaim_ReturnsEmptySet() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", null);
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when cognito:groups claim is not a List")
    void convert_WithNonListClaim_ReturnsEmptySet() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", "Admins"); // String instead of List
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when cognito:groups is an empty list")
    void convert_WithEmptyList_ReturnsEmptySet() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", List.of());
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should handle duplicate groups by returning unique authorities")
    void convert_WithDuplicateGroups_ReturnsUniqueAuthorities() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("cognito:groups", Arrays.asList("Admins", "Users", "Admins", "Users"));
        Jwt jwt = createJwt(claims);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then - Should deduplicate because result is a Set
        assertThat(authorities).hasSize(2);
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_Admins"),
            new SimpleGrantedAuthority("ROLE_Users")
        );
    }

    private Jwt createJwt(Map<String, Object> claims) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        
        return new Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            headers,
            claims
        );
    }
}
