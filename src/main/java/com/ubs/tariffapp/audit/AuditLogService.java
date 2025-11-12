package com.ubs.tariffapp.audit;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.repositories.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(Object entity, String changeType) {
        AuditLog log = new AuditLog();
        log.setChangeType(changeType);
        log.setChangedBy(getCurrentUsername());
        log.setChangeDate(LocalDateTime.now());
        log.setTargetEntity(entity.getClass().getSimpleName());

        if (entity instanceof TariffSchedule ts) {
            log.setTariffId(ts.getTariffId());
            log.setChangeDetails(entity.getClass().getSimpleName() + " " + changeType + ": " + ts.getTariffId());
        } else if (entity instanceof Duty duty) {
            TariffSchedule ts = duty.getTariffSchedule();
            log.setTariffId(ts.getTariffId());
            log.setChangeDetails(entity.getClass().getSimpleName() + " " + changeType +
                                 " (tariffId=" + ts.getTariffId() + ")");
        }

        auditLogRepository.save(log);
    }

    // Extract username from JWT token (Cognito)
    // Priority: username > cognito:username > email > preferred_username > sub (UUID fallback)
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) {
            return "system";
        }
        
        // If it's a JWT token, extract user-friendly claims
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // Try to get username from Cognito claims (in order of preference)
            String username = jwt.getClaimAsString("username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            
            String cognitoUsername = jwt.getClaimAsString("cognito:username");
            if (cognitoUsername != null && !cognitoUsername.isBlank()) {
                return cognitoUsername;
            }
            
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            
            // Fallback to sub (UUID) if nothing else available
            return jwt.getSubject();
        }
        
        // Fallback for non-JWT authentication
        return auth.getName();
    }
}
