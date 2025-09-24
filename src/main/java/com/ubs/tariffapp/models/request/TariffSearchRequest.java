package com.ubs.tariffapp.models.request;

public class TariffSearchRequest {
    private String reporterCode;
    private String partnerCode;
    private String productCode;
    private int year;

    // Constructors
    public TariffSearchRequest() {}

    public TariffSearchRequest(String reporterCode, String partnerCode, String productCode, int year) {
        this.reporterCode = reporterCode;
        this.partnerCode = partnerCode;
        this.productCode = productCode;
        this.year = year;
    }

    // Getters and Setters
    public String getReporterCode() { return reporterCode; }
    public void setReporterCode(String reporterCode) { this.reporterCode = reporterCode; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}