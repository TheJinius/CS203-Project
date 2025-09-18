package com.ubs.tariffapp.audit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for audit logging.
 * This ensures the AuditListener has access to the ApplicationContext
 * through dependency injection.
 */
@Configuration
public class AuditConfiguration {

    /**
     * Creates an AuditListener bean with ApplicationContext injected.
     * This sets up the static reference needed for JPA entity listeners
     * during application startup.
     * 
     * It is stored in the static field of AuditListener class, so it is
     * accessible even from non-Spring-managed instances created by JPA.
     */
    @Bean
    public AuditListener auditListener(ApplicationContext applicationContext) {
        AuditListener listener = new AuditListener();
        listener.setApplicationContext(applicationContext);
        return listener;
    }
}