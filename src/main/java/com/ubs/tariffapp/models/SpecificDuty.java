package com.ubs.tariffapp.models;

import java.math.BigDecimal;

import jakarta.persistence.Entity;

@Entity
public class SpecificDuty extends Duty {
    private BigDecimal amount; // For exact float precision
    private String unit;
    private Integer multiplier;

    public SpecificDuty() {
    }

    public SpecificDuty(Integer tariffId, TariffSchedule tariffSchedule, String dutyNature, String mathExpression,
            String rawText, BigDecimal amount, String unit, Integer multiplier) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression, rawText);
        this.amount = amount;
        this.unit = unit;
        this.multiplier = multiplier;
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
}
