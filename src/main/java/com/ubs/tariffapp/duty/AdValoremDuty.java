package com.ubs.tariffapp.duty;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
public class AdValoremDuty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rateId;

    private Double rateValue;
    private String rateType;
    private String unit;
    private int amountMin;
    private int amountCap;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String conditions;

    @ManyToOne
    @JoinColumn(name = "tariff_id")
    private Duty tariff;

    // Default constructor
    public AdValoremDuty() {
    }

    // Constructor with fields
    public AdValoremDuty(Double rateValue, String rateType, String unit, int amountMin, int amountCap, LocalDate validFrom,
            LocalDate validTo, String conditions, Duty tariff) {
        this.rateValue = rateValue;
        this.rateType = rateType;
        this.unit = unit;
        this.amountMin = amountMin;
        this.amountCap = amountCap;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.conditions = conditions;
        this.tariff = tariff;
    }

    public Long getRateId() {
        return rateId;
    }

    public void setRateId(Long rateId) {
        this.rateId = rateId;
    }

    public Double getRateValue() {
        return rateValue;
    }

    public void setRateValue(Double rateValue) {
        this.rateValue = rateValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public int getAmountMin() {
        return amountMin;
    }

    public void setAmountMin(int amountMin) {
        this.amountMin = amountMin;
    }

    public int getAmountCap() {
        return amountCap;
    }

    public void setAmountCap(int amountCap) {
        this.amountCap = amountCap;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public Duty getTariff() {
        return tariff;
    }

    public void setTariff(Duty tariff) {
        this.tariff = tariff;
    }
}
