package com.ubs.tariffapp.audit;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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

    @Transactional
    public void logChange(Object entity, String changeType) {
        System.out.println("AuditLogService.logChange called: " + changeType + " for " + entity.getClass().getSimpleName());
        
        AuditLog log = new AuditLog();
        log.setChangeType(changeType);
        log.setChangeDate(LocalDateTime.now());
        log.setChangedBy(getCurrentUsername());

        if (entity instanceof TariffSchedule ts) {
            log.setTariffId(ts.getTariffId());
            log.setChangeDetails("TariffSchedule " + changeType + ": " + ts.getTariffId());
        } else if (entity instanceof Duty duty) {
            TariffSchedule ts = duty.getTariffSchedule();
            log.setTariffId(ts.getTariffId());
            log.setChangeDetails(duty.getClass().getSimpleName() + " " + changeType +
                                 " (tariffId=" + ts.getTariffId() + ")");
        }

        auditLogRepository.save(log);
    }

    // TODO: Integrate with Spring Security to get actual username
    // When a user authenticates (e.g., via JWT, OAuth2, or username/password), 
    // Spring Security populates the SecurityContextHolder
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "system";
    }
}
