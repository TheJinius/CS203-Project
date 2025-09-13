package com.ubs.tariffapp.audit;

/**
 * Event class for audit logging.
 * This event is published when an entity is created, updated, or deleted.
 * It is handled asynchronously after the transaction commits to avoid
 * ConcurrentModificationException during Hibernate flush operations.
 */
public class AuditEvent {
    
    private final Object entity;
    private final String changeType;
    
    public AuditEvent(Object entity, String changeType) {
        this.entity = entity;
        this.changeType = changeType;
    }
    
    public Object getEntity() {
        return entity;
    }
    
    public String getChangeType() {
        return changeType;
    }
}