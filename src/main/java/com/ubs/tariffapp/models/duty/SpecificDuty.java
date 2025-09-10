package com.ubs.tariffapp.models.duty;

import java.math.BigDecimal;

import com.ubs.tariffapp.models.TariffSchedule;

import jakarta.persistence.Entity;

@Entity
public class SpecificDuty extends Duty {
    private BigDecimal amount; // For exact float precision
    private String unit;
    private Integer multiplier;
    private String specificDutyRateRaw;

    public SpecificDuty() {
    }

    public SpecificDuty(Integer tariffId, TariffSchedule tariffSchedule, String dutyNature, String mathExpression,
            BigDecimal amount, String unit, Integer multiplier, String specificDutyRateRaw) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression);
        // E.g. for 5 USD/100 kg": amount = 5, unit = "kg", multiplier = 100
        this.amount = amount; 
        this.unit = unit;
        this.multiplier = multiplier;
        this.specificDutyRateRaw = specificDutyRateRaw;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Integer multiplier) {
        this.multiplier = multiplier;
    }

    public String getSpecificDutyRateRaw() {
        return specificDutyRateRaw;
    }
    
    public void setSpecificDutyRateRaw(String specificDutyRateRaw) {
        this.specificDutyRateRaw = specificDutyRateRaw;
    }
}
