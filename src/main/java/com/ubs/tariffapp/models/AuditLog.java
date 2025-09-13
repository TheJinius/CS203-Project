package com.ubs.tariffapp.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer logId;

    @ManyToOne
    @JoinColumn(name = "tariff_id", nullable = false)
    private TariffSchedule tariffSchedule;

    // No-argument constructor
    public AuditLog() {
    }

    // All-argument constructor
    public AuditLog(Integer logId, TariffSchedule tariffSchedule,
            String targetEntity, String changeType, String changedBy,
            LocalDateTime changeDate, String changeDetails) {
        this.logId = logId;
        this.tariffSchedule = tariffSchedule;
        this.targetEntity = targetEntity;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.changeDate = changeDate;
        this.changeDetails = changeDetails;
    }

    private String targetEntity; // e.g., "TariffSchedule", "AdValoremDuty"
    private String changeType; // INSERT / UPDATE / DELETE
    private String changedBy;
    private LocalDateTime changeDate;
    private String changeDetails;

    // Getters and setters
    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public TariffSchedule getTariffSchedule() {
        return tariffSchedule;
    }

    public void setTariffSchedule(TariffSchedule tariffSchedule) {
        this.tariffSchedule = tariffSchedule;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getChangeDetails() {
        return changeDetails;
    }

    public void setChangeDetails(String changeDetails) {
        this.changeDetails = changeDetails;
    }
}
