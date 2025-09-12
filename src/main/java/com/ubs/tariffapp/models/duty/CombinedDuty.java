package com.ubs.tariffapp.models.duty;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;

import com.ubs.tariffapp.models.TariffSchedule;

@Entity
public class CombinedDuty extends Duty {
    @Column(name = "mixed_or_conditional", columnDefinition = "CHAR(1)")
    private String mixedOrConditional;
    
    private BigDecimal ratePercent; // For exact float precision
    private BigDecimal amount; // For exact float precision
    private String unit;
    private Integer multiplier;
    private String specificDutyRateRaw;

    public CombinedDuty() {
    }

    public CombinedDuty(Integer tariffId, TariffSchedule tariffSchedule, String dutyNature, String mathExpression,
            String mixedOrConditional, BigDecimal ratePercent, BigDecimal amount, String unit,
            Integer multiplier, String specificDutyRateRaw) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression);
        this.mixedOrConditional = mixedOrConditional;
        this.ratePercent = ratePercent;
        this.amount = amount;
        this.unit = unit;
        this.multiplier = multiplier;
        this.specificDutyRateRaw = specificDutyRateRaw;
    }

    public String getMixedOrConditional() {
        return mixedOrConditional;
    }

    public void setMixedOrConditional(String mixedOrConditional) {
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
