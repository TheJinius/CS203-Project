package com.ubs.tariffapp.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for UserController
 * Tests user profile retrieval and health check endpoints with various authentication scenarios
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/user/profile - Get User Profile")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should return user profile with email, name, and groups for authenticated user")
        void testGetUserProfile_Success() throws Exception {
            // Arrange
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user123");
            claims.put("email", "john.doe@example.com");
            claims.put("name", "John Doe");
            claims.put("cognito:groups", Arrays.asList("Users", "Developers"));

            OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
            );

            OidcUser oidcUser = new DefaultOidcUser(
                Arrays.asList(new SimpleGrantedAuthority("ROLE_Users")),
                idToken
            );

            // Act & Assert
            mockMvc.perform(get("/api/user/profile")
                    .with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.groups").isArray())
                    .andExpect(jsonPath("$.groups", hasSize(2)))
                    .andExpect(jsonPath("$.groups[0]").value("Users"))
                    .andExpect(jsonPath("$.groups[1]").value("Developers"));
        }

        @Test
        @DisplayName("Should return user profile with empty groups when no groups claim exists")
        void testGetUserProfile_NoGroups() throws Exception {
            // Arrange
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user456");
            claims.put("email", "jane.smith@example.com");
            claims.put("name", "Jane Smith");

            OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
            );

            OidcUser oidcUser = new DefaultOidcUser(
                Arrays.asList(new SimpleGrantedAuthority("ROLE_Users")),
                idToken
            );

            // Act & Assert
            mockMvc.perform(get("/api/user/profile")
                    .with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
                    .andExpect(jsonPath("$.name").value("Jane Smith"))
                    .andExpect(jsonPath("$.groups").isArray())
                    .andExpect(jsonPath("$.groups", hasSize(0)));
        }

        @Test
        @DisplayName("Should allow admin users to access user profile")
        void testGetUserProfile_AdminAccess() throws Exception {
            // Arrange
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "admin123");
            claims.put("email", "admin@example.com");
            claims.put("name", "Admin User");
            claims.put("cognito:groups", Arrays.asList("Admins"));

            OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
            );

            OidcUser oidcUser = new DefaultOidcUser(
                Arrays.asList(new SimpleGrantedAuthority("ROLE_Admins")),
                idToken
            );

            // Act & Assert
            mockMvc.perform(get("/api/user/profile")
                    .with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("admin@example.com"))
                    .andExpect(jsonPath("$.name").value("Admin User"));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void testGetUserProfile_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/user/profile"))
                    .andExpect(status().isUnauthorized());
        }
        
        @Test
        @DisplayName("Should handle user with multiple groups")
        void testGetUserProfile_MultipleGroups() throws Exception {
            // Arrange
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user789");
            claims.put("email", "multi.role@example.com");
            claims.put("name", "Multi Role User");
            claims.put("cognito:groups", Arrays.asList("Users", "Admins", "Developers", "Managers"));

            OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
            );

            OidcUser oidcUser = new DefaultOidcUser(
                Arrays.asList(new SimpleGrantedAuthority("ROLE_Users")),
                idToken
            );

            // Act & Assert
            mockMvc.perform(get("/api/user/profile")
                    .with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("multi.role@example.com"))
                    .andExpect(jsonPath("$.name").value("Multi Role User"))
                    .andExpect(jsonPath("$.groups").isArray())
                    .andExpect(jsonPath("$.groups", hasSize(4)))
                    .andExpect(jsonPath("$.groups", containsInAnyOrder("Users", "Admins", "Developers", "Managers")));
        }
    }
}
