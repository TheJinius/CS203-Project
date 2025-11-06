package com.ubs.tariffapp.models.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lombok removed to avoid dependency errors; explicit getters/setters added
public class TariffRequest {
    // Validation groups for different operations
    public interface Create {}
    public interface Update {}

    @NotNull(message = "Tariff year is required", groups = Create.class)
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer tariffYear;
    
    @NotBlank(message = "Reporter code is required", groups = Create.class)
    @Size(min = 3, max = 3, message = "Reporter code must be exactly 3 characters")
    private String reporterCode;
    
    @NotBlank(message = "Partner code is required", groups = Create.class)
    @Size(min = 3, max = 3, message = "Partner code must be exactly 3 characters")
    private String partnerCode;
    
    @NotBlank(message = "Product code (TL code) is required", groups = Create.class)
    private String tlCode;
    
    @NotBlank(message = "Duty type is required", groups = Create.class)
    private String dutyType;
    
    @NotBlank(message = "Duty code is required", groups = Create.class)
    private String dutyCode;

    // ✅ Editable fields (always allowed)
    private String tlsSuffix;
    private String note;
    private String specificRateUnit;  // Liberal - accepts any string value

    // ✅ Optional rate fields with validation
    @DecimalMin(value = "0.0", message = "Ad valorem rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Ad valorem rate cannot exceed 100%")
    private Double adValoremRate;
    
    @DecimalMin(value = "0.0", message = "Specific rate must be non-negative")
    private Double specificRate;
    
    @DecimalMin(value = "0.0", message = "Compound rate 1 must be non-negative")
    private Double compoundRate1;
    
    @DecimalMin(value = "0.0", message = "Compound rate 2 must be non-negative")
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
