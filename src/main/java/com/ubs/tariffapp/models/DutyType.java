
package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class DutyType {
    @EmbeddedId
    private DutyTypeId id;
    private String dutyTypeDescription;

    @OneToMany(mappedBy = "dutyType")
    private List<TariffSchedule> tariffSchedules;

    public DutyType() {
    }

    public DutyType(DutyTypeId id, String dutyTypeDescription, List<TariffSchedule> tariffSchedules) {
        this.id = id;
        this.dutyTypeDescription = dutyTypeDescription;
        this.tariffSchedules = tariffSchedules;
    }

    // getters and setters
    public DutyTypeId getId() {
        return id;
    }

    public void setId(DutyTypeId id) {
        this.id = id;
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
