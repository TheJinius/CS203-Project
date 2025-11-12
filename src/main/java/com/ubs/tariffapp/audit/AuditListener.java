package com.ubs.tariffapp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * JPA entity listeners are not Spring managed beans by default.
 * We use ApplicationContextAware to get access to Spring beans
 * from within JPA entity listener methods.
 */

@Component // Make Spring manage this bean
public class AuditListener implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(AuditListener.class);
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        AuditListener.applicationContext = context;
    }

    public AuditListener() {
        // Default constructor required by JPA
    }

    public AuditListener(AuditLogService service) {
        // This constructor is for Spring configuration
    }

    // Helper to get AuditLogService bean from ApplicationContext
    private AuditLogService getAuditLogService() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(AuditLogService.class);
            } catch (NoSuchBeanDefinitionException e) {
                // AuditLogService bean not available, which is fine for some test contexts
                return null;
            }
        }
        return null;
    }

    @PostPersist
    public void postPersist(Object entity) {
        AuditLogService auditService = getAuditLogService();
        if (auditService != null) {
            try {
                auditService.logChange(entity, "INSERT");
            } catch (Exception e) {
                log.error("Failed to create audit log for INSERT on {}: {}", 
                    entity.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        AuditLogService auditService = getAuditLogService();
        if (auditService != null) {
            try {
                auditService.logChange(entity, "UPDATE");
            } catch (Exception e) {
                log.error("Failed to create audit log for UPDATE on {}: {}", 
                    entity.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        AuditLogService auditService = getAuditLogService();
        if (auditService != null) {
            try {
                auditService.logChange(entity, "DELETE");
            } catch (Exception e) {
                log.error("Failed to create audit log for DELETE on {}: {}", 
                    entity.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}
