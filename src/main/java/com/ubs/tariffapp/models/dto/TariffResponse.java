package com.ubs.tariffapp.models.dto;

public class TariffResponse {
    private Integer tariffId;
    private Integer tariffYear;
    
    // Country information
    private String reporterCode;
    private String reporterName;
    private String partnerCode;
    private String partnerName;
    
    // Product information
    private String tlCode;
    private String productDescription;
    
    // Duty type information
    private String dutyType;
    private String dutyCode;
    private String dutyTypeDescription;
    
    private String tlsSuffix;
    private String note;
    
    // Duty details
    private String dutyCategory; // AD_VALOREM, SPECIFIC, COMBINED, etc.
    private Double adValoremRate;
    private Double specificRate;
    private String specificRateUnit;
    private Double compoundRate1;
    private Double compoundRate2;

    // Default constructor
    public TariffResponse() {}

    // All-args constructor
    public TariffResponse(Integer tariffId, Integer tariffYear, String reporterCode, String reporterName,
                         String partnerCode, String partnerName, String tlCode, String productDescription,
                         String dutyType, String dutyCode, String dutyTypeDescription, String tlsSuffix,
                         String note, String dutyCategory, Double adValoremRate, Double specificRate,
                         String specificRateUnit, Double compoundRate1, Double compoundRate2) {
        this.tariffId = tariffId;
        this.tariffYear = tariffYear;
        this.reporterCode = reporterCode;
        this.reporterName = reporterName;
        this.partnerCode = partnerCode;
        this.partnerName = partnerName;
        this.tlCode = tlCode;
        this.productDescription = productDescription;
        this.dutyType = dutyType;
        this.dutyCode = dutyCode;
        this.dutyTypeDescription = dutyTypeDescription;
        this.tlsSuffix = tlsSuffix;
        this.note = note;
        this.dutyCategory = dutyCategory;
        this.adValoremRate = adValoremRate;
        this.specificRate = specificRate;
        this.specificRateUnit = specificRateUnit;
        this.compoundRate1 = compoundRate1;
        this.compoundRate2 = compoundRate2;
    }

    // Builder pattern (manual implementation)
    public static TariffResponseBuilder builder() {
        return new TariffResponseBuilder();
    }

    public static class TariffResponseBuilder {
        private TariffResponse response = new TariffResponse();

        public TariffResponseBuilder tariffId(Integer tariffId) { response.tariffId = tariffId; return this; }
        public TariffResponseBuilder tariffYear(Integer tariffYear) { response.tariffYear = tariffYear; return this; }
        public TariffResponseBuilder reporterCode(String reporterCode) { response.reporterCode = reporterCode; return this; }
        public TariffResponseBuilder reporterName(String reporterName) { response.reporterName = reporterName; return this; }
        public TariffResponseBuilder partnerCode(String partnerCode) { response.partnerCode = partnerCode; return this; }
        public TariffResponseBuilder partnerName(String partnerName) { response.partnerName = partnerName; return this; }
        public TariffResponseBuilder tlCode(String tlCode) { response.tlCode = tlCode; return this; }
        public TariffResponseBuilder productDescription(String productDescription) { response.productDescription = productDescription; return this; }
        public TariffResponseBuilder dutyType(String dutyType) { response.dutyType = dutyType; return this; }
        public TariffResponseBuilder dutyCode(String dutyCode) { response.dutyCode = dutyCode; return this; }
        public TariffResponseBuilder dutyTypeDescription(String dutyTypeDescription) { response.dutyTypeDescription = dutyTypeDescription; return this; }
        public TariffResponseBuilder tlsSuffix(String tlsSuffix) { response.tlsSuffix = tlsSuffix; return this; }
        public TariffResponseBuilder note(String note) { response.note = note; return this; }
        public TariffResponseBuilder dutyCategory(String dutyCategory) { response.dutyCategory = dutyCategory; return this; }
        public TariffResponseBuilder adValoremRate(Double adValoremRate) { response.adValoremRate = adValoremRate; return this; }
        public TariffResponseBuilder specificRate(Double specificRate) { response.specificRate = specificRate; return this; }
        public TariffResponseBuilder specificRateUnit(String specificRateUnit) { response.specificRateUnit = specificRateUnit; return this; }
        public TariffResponseBuilder compoundRate1(Double compoundRate1) { response.compoundRate1 = compoundRate1; return this; }
        public TariffResponseBuilder compoundRate2(Double compoundRate2) { response.compoundRate2 = compoundRate2; return this; }

        public TariffResponse build() { return response; }
    }

    // Getters
    public Integer getTariffId() { return tariffId; }
    public Integer getTariffYear() { return tariffYear; }
    public String getReporterCode() { return reporterCode; }
    public String getReporterName() { return reporterName; }
    public String getPartnerCode() { return partnerCode; }
    public String getPartnerName() { return partnerName; }
    public String getTlCode() { return tlCode; }
    public String getProductDescription() { return productDescription; }
    public String getDutyType() { return dutyType; }
    public String getDutyCode() { return dutyCode; }
    public String getDutyTypeDescription() { return dutyTypeDescription; }
    public String getTlsSuffix() { return tlsSuffix; }
    public String getNote() { return note; }
    public String getDutyCategory() { return dutyCategory; }
    public Double getAdValoremRate() { return adValoremRate; }
    public Double getSpecificRate() { return specificRate; }
    public String getSpecificRateUnit() { return specificRateUnit; }
    public Double getCompoundRate1() { return compoundRate1; }
    public Double getCompoundRate2() { return compoundRate2; }

    // Setters
    public void setTariffId(Integer tariffId) { this.tariffId = tariffId; }
    public void setTariffYear(Integer tariffYear) { this.tariffYear = tariffYear; }
    public void setReporterCode(String reporterCode) { this.reporterCode = reporterCode; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public void setTlCode(String tlCode) { this.tlCode = tlCode; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public void setDutyType(String dutyType) { this.dutyType = dutyType; }
    public void setDutyCode(String dutyCode) { this.dutyCode = dutyCode; }
    public void setDutyTypeDescription(String dutyTypeDescription) { this.dutyTypeDescription = dutyTypeDescription; }
    public void setTlsSuffix(String tlsSuffix) { this.tlsSuffix = tlsSuffix; }
    public void setNote(String note) { this.note = note; }
    public void setDutyCategory(String dutyCategory) { this.dutyCategory = dutyCategory; }
    public void setAdValoremRate(Double adValoremRate) { this.adValoremRate = adValoremRate; }
    public void setSpecificRate(Double specificRate) { this.specificRate = specificRate; }
    public void setSpecificRateUnit(String specificRateUnit) { this.specificRateUnit = specificRateUnit; }
    public void setCompoundRate1(Double compoundRate1) { this.compoundRate1 = compoundRate1; }
    public void setCompoundRate2(Double compoundRate2) { this.compoundRate2 = compoundRate2; }
}
