package com.ubs.tariffapp.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * static reference to the Spring-managed service instead of trying to
 * inject a new instance. auditLogService is static, so all instances of
 * AuditListener (including those created by JPA) share it.
 */

@Component // Make Spring manage this bean
public class AuditListener {

    private static AuditLogService auditLogService;

    public AuditListener() {
        // Default constructor required by JPA
    }

    @Autowired
    public AuditListener(AuditLogService service) {
        AuditListener.auditLogService = service; // static reference
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (auditLogService != null) {
            auditLogService.logChange(entity, "INSERT");
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (auditLogService != null) {
            auditLogService.logChange(entity, "UPDATE");
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        if (auditLogService != null) {
            auditLogService.logChange(entity, "DELETE");
        }
    }
}
