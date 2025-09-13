package com.ubs.tariffapp.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for audit logging.
 * This ensures the AuditListener has access to the AuditLogService
 * through dependency injection.
 */
@Configuration
public class AuditConfiguration {

    /**
     * Creates an AuditListener bean with AuditLogService injected.
     * This sets up the static reference needed for JPA entity listeners.
     */
    @Bean
    public AuditListener auditListener(AuditLogService auditLogService) {
        return new AuditListener(auditLogService);
    }
}