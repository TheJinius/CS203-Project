package com.ubs.tariffapp.models;

import java.math.BigDecimal;

import jakarta.persistence.Entity;

@Entity
public class CombinedDuty extends Duty {
    private char mixedOrConditional;
    private BigDecimal ratePercent; // For exact float precision
    private BigDecimal amount; // For exact float precision
    private String unit;
    private Integer multiplier;
    private String specificDutyRateRaw;

    public CombinedDuty() {
    }

    public CombinedDuty(Integer tariffId, TariffSchedule tariffSchedule, String dutyNature, String mathExpression,
            char mixedOrConditional, BigDecimal ratePercent, BigDecimal amount, String unit,
            Integer multiplier, String specificDutyRateRaw) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression);
        this.mixedOrConditional = mixedOrConditional;
        this.ratePercent = ratePercent;
        this.amount = amount;
        this.unit = unit;
        this.multiplier = multiplier;
        this.specificDutyRateRaw = specificDutyRateRaw;
    }

    public char getMixedOrConditional() {
        return mixedOrConditional;
    }

    public void setMixedOrConditional(char mixedOrConditional) {
        this.mixedOrConditional = mixedOrConditional;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
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
