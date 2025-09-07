package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class DutyType {
    @Id
    private String dutyTypeCode;
    private String dutyTypeDescription;

    @OneToMany(mappedBy = "dutyType")
    private List<TariffSchedule> tariffSchedules;

    // No-argument constructor
    public DutyType() {
    }

    // All-argument constructor
    public DutyType(String dutyTypeCode, String dutyTypeDescription, List<TariffSchedule> tariffSchedules) {
        this.dutyTypeCode = dutyTypeCode;
        this.dutyTypeDescription = dutyTypeDescription;
        this.tariffSchedules = tariffSchedules;
    }

    // Getters and setters
    public String getDutyTypeCode() {
        return dutyTypeCode;
    }

    public void setDutyTypeCode(String dutyTypeCode) {
        this.dutyTypeCode = dutyTypeCode;
    }

    public String getDutyTypeDescription() {
        return dutyTypeDescription;
    }

    public void setDutyTypeDescription(String dutyTypeDescription) {
        this.dutyTypeDescription = dutyTypeDescription;
    }

    public List<TariffSchedule> getTariffSchedules() {
        return tariffSchedules;
    }

    public void setTariffSchedules(List<TariffSchedule> tariffSchedules) {
        this.tariffSchedules = tariffSchedules;
    }
}
