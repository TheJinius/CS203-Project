package com.ubs.tariffapp.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

// Base class for different types of duties
// Adheres to the total specialisation rule for completeness constraint
// In other words, each instance of Duty must be one of its subclasses
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Duty {
    @Id
    private Integer tariffId;

    @OneToOne
    @JoinColumn(name = "tariff_id")
    private TariffSchedule tariffSchedule;

    private String dutyNature;
    private String mathExpression;

    public Duty() {
    }

    public Duty(Integer tariffId, TariffSchedule tariffSchedule,
            String dutyNature, String mathExpression) {
        this.tariffId = tariffId;
        this.tariffSchedule = tariffSchedule;
        this.dutyNature = dutyNature;
        this.mathExpression = mathExpression;
    }

    // getters and setters
    public Integer getTariffId() {
        return tariffId;
    }

    public void setTariffId(Integer tariffId) {
        this.tariffId = tariffId;
    }

    public TariffSchedule getTariffSchedule() {
        return tariffSchedule;
    }

    public void setTariffSchedule(TariffSchedule tariffSchedule) {
        this.tariffSchedule = tariffSchedule;
    }

    public String getDutyNature() {
        return dutyNature;
    }

    public void setDutyNature(String dutyNature) {
        this.dutyNature = dutyNature;
    }

    public String getMathExpression() {
        return mathExpression;
    }

    public void setMathExpression(String mathExpression) {
        this.mathExpression = mathExpression;
    }

}
