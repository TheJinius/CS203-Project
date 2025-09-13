package com.ubs.tariffapp.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * JPA entity listeners are not Spring managed beans by default.
 * So constructor injection via @Autowired does not work.
 * 
 * We use a static reference to the AuditLogService, which is set in the
 * constructor, together with @Component to make Spring manage this bean.
 * When JPA calls a listener instance, it uses the
 * static reference to register a transaction synchronization that executes
 * just before commit to avoid ConcurrentModificationException.
 */

@Component // Make Spring manage this bean
public class AuditListener {

    private static AuditLogService auditLogService;

    public AuditListener() {
        // Default constructor required by JPA
    }

    public AuditListener(AuditLogService service) {
        System.out.println("AuditListener constructor called with service: " + (service != null ? "NOT NULL" : "NULL"));
        AuditListener.auditLogService = service; // static reference
        System.out.println("Static auditLogService set to: " + (AuditListener.auditLogService != null ? "NOT NULL" : "NULL"));
    }

    @PostPersist
    public void postPersist(Object entity) {
        System.out.println("AuditListener.postPersist called for: " + entity.getClass().getSimpleName());
        System.out.println("auditLogService is null: " + (auditLogService == null));
        System.out.println("TransactionSynchronizationManager.isSynchronizationActive(): " + TransactionSynchronizationManager.isSynchronizationActive());
        
        if (auditLogService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            System.out.println("Registering transaction synchronization for INSERT");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    System.out.println("beforeCommit called, readOnly: " + readOnly);
                    if (!readOnly) {
                        System.out.println("Calling auditLogService.logChange for INSERT");
                        auditLogService.logChange(entity, "INSERT");
                    }
                }
            });
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (auditLogService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    if (!readOnly) {
                        auditLogService.logChange(entity, "UPDATE");
                    }
                }
            });
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        if (auditLogService != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    if (!readOnly) {
                        auditLogService.logChange(entity, "DELETE");
                    }
                }
            });
        }
    }
}
