package com.ubs.tariffapp.models;

import java.math.BigDecimal;

import jakarta.persistence.Entity;

@Entity
public class AdValoremDuty extends Duty {
    private BigDecimal ratePercent; // For exact float precision

    public AdValoremDuty() {
    }

    public AdValoremDuty(Integer tariffId, TariffSchedule tariffSchedule, String dutyNature, String mathExpression,
            BigDecimal ratePercent) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression);
        this.ratePercent = ratePercent;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
    }
}
