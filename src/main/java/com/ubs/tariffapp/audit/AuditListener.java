package com.ubs.tariffapp.audit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            return applicationContext.getBean(AuditLogService.class);
        }
        return null;
    }

    @PostPersist
    public void postPersist(Object entity) {
        AuditLogService auditService = getAuditLogService();
        
        if (auditService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    if (!readOnly) {
                        auditService.logChange(entity, "INSERT");
                    }
                }
            });
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        AuditLogService auditService = getAuditLogService();
        if (auditService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    if (!readOnly) {
                        auditService.logChange(entity, "UPDATE");
                    }
                }
            });
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        AuditLogService auditService = getAuditLogService();
        if (auditService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    if (!readOnly) {
                        auditService.logChange(entity, "DELETE");
                    }
                }
            });
        }
    }
}
