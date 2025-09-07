package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Product {
    @Id
    private String tlCode;
    private String description;
    private String hsVersion;
    private Integer digits;

    @OneToMany(mappedBy = "product")
    private List<TariffSchedule> tariffSchedules;

    // No-argument constructor
    public Product() {
    }

    // All-argument constructor
    public Product(String tlCode, String description, String hsVersion, Integer digits,
            List<TariffSchedule> tariffSchedules) {
        this.tlCode = tlCode;
        this.description = description;
        this.hsVersion = hsVersion;
        this.digits = digits;
        this.tariffSchedules = tariffSchedules;
    }

    // Getters and setters
    public String getTlCode() {
        return tlCode;
    }

    public void setTlCode(String tlCode) {
        this.tlCode = tlCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHsVersion() {
        return hsVersion;
    }

    public void setHsVersion(String hsVersion) {
        this.hsVersion = hsVersion;
    }

    public Integer getDigits() {
        return digits;
    }

    public void setDigits(Integer digits) {
        this.digits = digits;
    }

    public List<TariffSchedule> getTariffSchedules() {
        return tariffSchedules;
    }

    public void setTariffSchedules(List<TariffSchedule> tariffSchedules) {
        this.tariffSchedules = tariffSchedules;
    }
}
