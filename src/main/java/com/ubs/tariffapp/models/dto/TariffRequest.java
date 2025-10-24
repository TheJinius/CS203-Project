package com.ubs.tariffapp.models.dto;

// Lombok removed to avoid dependency errors; explicit getters/setters added
public class TariffRequest {
    // ❌ REMOVE @NotNull from immutable fields for UPDATE operations
    // ❌ Or make them optional with groups

    private Integer tariffYear;        // Optional for updates
    private String reporterCode;       // Optional for updates
    private String partnerCode;        // Optional for updates
    private String tlCode;             // Optional for updates
    private String dutyType;           // Optional for updates
    private String dutyCode;           // Optional for updates

    // ✅ Editable fields (always allowed)
    private String tlsSuffix;
    private String note;
    private String specificRateUnit;

    // ✅ Optional rate fields
    private Double adValoremRate;
    private Double specificRate;
    private Double compoundRate1;
    private Double compoundRate2;

    public Integer getTariffYear() {
        return tariffYear;
    }

    public void setTariffYear(Integer tariffYear) {
        this.tariffYear = tariffYear;
    }

    public String getReporterCode() {
        return reporterCode;
    }

    public void setReporterCode(String reporterCode) {
        this.reporterCode = reporterCode;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getTlCode() {
        return tlCode;
    }

    public void setTlCode(String tlCode) {
        this.tlCode = tlCode;
    }

    public String getDutyType() {
        return dutyType;
    }

    public void setDutyType(String dutyType) {
        this.dutyType = dutyType;
    }

    public String getDutyCode() {
        return dutyCode;
    }

    public void setDutyCode(String dutyCode) {
        this.dutyCode = dutyCode;
    }

    public String getTlsSuffix() {
        return tlsSuffix;
    }

    public void setTlsSuffix(String tlsSuffix) {
        this.tlsSuffix = tlsSuffix;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSpecificRateUnit() {
        return specificRateUnit;
    }

    public void setSpecificRateUnit(String specificRateUnit) {
        this.specificRateUnit = specificRateUnit;
    }

    public Double getAdValoremRate() {
        return adValoremRate;
    }

    public void setAdValoremRate(Double adValoremRate) {
        this.adValoremRate = adValoremRate;
    }

    public Double getSpecificRate() {
        return specificRate;
    }

    public void setSpecificRate(Double specificRate) {
        this.specificRate = specificRate;
    }

    public Double getCompoundRate1() {
        return compoundRate1;
    }

    public void setCompoundRate1(Double compoundRate1) {
        this.compoundRate1 = compoundRate1;
    }

    public Double getCompoundRate2() {
        return compoundRate2;
    }

    public void setCompoundRate2(Double compoundRate2) {
        this.compoundRate2 = compoundRate2;
    }
}
