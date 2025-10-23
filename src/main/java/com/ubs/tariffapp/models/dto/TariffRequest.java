package com.ubs.tariffapp.models.dto;

import jakarta.validation.constraints.*;

public class TariffRequest {
    
    @NotNull(message = "Tariff year is required")
    @Min(value = 2000, message = "Tariff year must be 2000 or later")
    @Max(value = 2100, message = "Tariff year must be before 2100")
    private Integer tariffYear;

    @NotBlank(message = "Reporter country code is required")
    @Size(min = 3, max = 3, message = "Reporter country code must be 3 characters")
    private String reporterCode;

    @NotBlank(message = "Partner country code is required")
    @Size(min = 3, max = 3, message = "Partner country code must be 3 characters")
    private String partnerCode;

    @NotBlank(message = "Product TL code is required")
    private String tlCode;

    @NotBlank(message = "Duty type is required")
    private String dutyType;

    @NotBlank(message = "Duty code is required")
    private String dutyCode;

    private String tlsSuffix;

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;

    // Duty details
    private Double adValoremRate;
    private Double specificRate;
    private String specificRateUnit;
    private Double compoundRate1;
    private Double compoundRate2;

    // Default constructor
    public TariffRequest() {}

    // All-args constructor
    public TariffRequest(Integer tariffYear, String reporterCode, String partnerCode, String tlCode,
                        String dutyType, String dutyCode, String tlsSuffix, String note,
                        Double adValoremRate, Double specificRate, String specificRateUnit,
                        Double compoundRate1, Double compoundRate2) {
        this.tariffYear = tariffYear;
        this.reporterCode = reporterCode;
        this.partnerCode = partnerCode;
        this.tlCode = tlCode;
        this.dutyType = dutyType;
        this.dutyCode = dutyCode;
        this.tlsSuffix = tlsSuffix;
        this.note = note;
        this.adValoremRate = adValoremRate;
        this.specificRate = specificRate;
        this.specificRateUnit = specificRateUnit;
        this.compoundRate1 = compoundRate1;
        this.compoundRate2 = compoundRate2;
    }

    // Builder pattern (manual implementation)
    public static TariffRequestBuilder builder() {
        return new TariffRequestBuilder();
    }

    public static class TariffRequestBuilder {
        private TariffRequest request = new TariffRequest();

        public TariffRequestBuilder tariffYear(Integer tariffYear) { request.tariffYear = tariffYear; return this; }
        public TariffRequestBuilder reporterCode(String reporterCode) { request.reporterCode = reporterCode; return this; }
        public TariffRequestBuilder partnerCode(String partnerCode) { request.partnerCode = partnerCode; return this; }
        public TariffRequestBuilder tlCode(String tlCode) { request.tlCode = tlCode; return this; }
        public TariffRequestBuilder dutyType(String dutyType) { request.dutyType = dutyType; return this; }
        public TariffRequestBuilder dutyCode(String dutyCode) { request.dutyCode = dutyCode; return this; }
        public TariffRequestBuilder tlsSuffix(String tlsSuffix) { request.tlsSuffix = tlsSuffix; return this; }
        public TariffRequestBuilder note(String note) { request.note = note; return this; }
        public TariffRequestBuilder adValoremRate(Double adValoremRate) { request.adValoremRate = adValoremRate; return this; }
        public TariffRequestBuilder specificRate(Double specificRate) { request.specificRate = specificRate; return this; }
        public TariffRequestBuilder specificRateUnit(String specificRateUnit) { request.specificRateUnit = specificRateUnit; return this; }
        public TariffRequestBuilder compoundRate1(Double compoundRate1) { request.compoundRate1 = compoundRate1; return this; }
        public TariffRequestBuilder compoundRate2(Double compoundRate2) { request.compoundRate2 = compoundRate2; return this; }

        public TariffRequest build() { return request; }
    }

    // Getters
    public Integer getTariffYear() { return tariffYear; }
    public String getReporterCode() { return reporterCode; }
    public String getPartnerCode() { return partnerCode; }
    public String getTlCode() { return tlCode; }
    public String getDutyType() { return dutyType; }
    public String getDutyCode() { return dutyCode; }
    public String getTlsSuffix() { return tlsSuffix; }
    public String getNote() { return note; }
    public Double getAdValoremRate() { return adValoremRate; }
    public Double getSpecificRate() { return specificRate; }
    public String getSpecificRateUnit() { return specificRateUnit; }
    public Double getCompoundRate1() { return compoundRate1; }
    public Double getCompoundRate2() { return compoundRate2; }

    // Setters
    public void setTariffYear(Integer tariffYear) { this.tariffYear = tariffYear; }
    public void setReporterCode(String reporterCode) { this.reporterCode = reporterCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public void setTlCode(String tlCode) { this.tlCode = tlCode; }
    public void setDutyType(String dutyType) { this.dutyType = dutyType; }
    public void setDutyCode(String dutyCode) { this.dutyCode = dutyCode; }
    public void setTlsSuffix(String tlsSuffix) { this.tlsSuffix = tlsSuffix; }
    public void setNote(String note) { this.note = note; }
    public void setAdValoremRate(Double adValoremRate) { this.adValoremRate = adValoremRate; }
    public void setSpecificRate(Double specificRate) { this.specificRate = specificRate; }
    public void setSpecificRateUnit(String specificRateUnit) { this.specificRateUnit = specificRateUnit; }
    public void setCompoundRate1(Double compoundRate1) { this.compoundRate1 = compoundRate1; }
    public void setCompoundRate2(Double compoundRate2) { this.compoundRate2 = compoundRate2; }
}
