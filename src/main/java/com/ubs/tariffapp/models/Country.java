package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Country {
    @Id
    private Integer countryId;
    private String countryName;
    private String isoCode;

    @OneToMany(mappedBy = "reporter")
    private List<TariffSchedule> reportedTariffs;

    @OneToMany(mappedBy = "partner")
    private List<TariffSchedule> partneredTariffs;

    // No-argument constructor
    public Country() {
    }

    // All-argument constructor
    public Country(Integer countryId, String countryName, String isoCode, List<TariffSchedule> reportedTariffs,
            List<TariffSchedule> partneredTariffs) {
        this.countryId = countryId;
        this.countryName = countryName;
        this.isoCode = isoCode;
        this.reportedTariffs = reportedTariffs;
        this.partneredTariffs = partneredTariffs;
    }

    // Getters and setters
    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public List<TariffSchedule> getReportedTariffs() {
        return reportedTariffs;
    }

    public void setReportedTariffs(List<TariffSchedule> reportedTariffs) {
        this.reportedTariffs = reportedTariffs;
    }

    public List<TariffSchedule> getPartneredTariffs() {
        return partneredTariffs;
    }

    public void setPartneredTariffs(List<TariffSchedule> partneredTariffs) {
        this.partneredTariffs = partneredTariffs;
    }
}
