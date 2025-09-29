package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ubs.tariffapp.models.UserProfile;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('Users', 'Admins')")
    public ResponseEntity<UserProfile> getUserProfile(Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        
        UserProfile profile = new UserProfile(oidcUser.getEmail(), oidcUser.getFullName(), extractGroups(oidcUser));

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/api/admin/")
    @PreAuthorize("hasRole('Admins')")
    public ResponseEntity<String> adminHealthCheck() {
        // Admin-only endpoint
        return ResponseEntity.ok("Admin endpoint healthcheck: OK");
    }

    @GetMapping("/api/user/")
    @PreAuthorize("hasRole('Users')")
    public ResponseEntity<String> userHealthCheck() {
        // Admin-only endpoint
        return ResponseEntity.ok("User endpoint healthcheck: OK");
    }

    private List<String> extractGroups(OidcUser oidcUser) {
        Object groups = oidcUser.getIdToken().getClaim("cognito:groups");
        return groups instanceof List ? (List<String>) groups : Collections.emptyList();
    }
}