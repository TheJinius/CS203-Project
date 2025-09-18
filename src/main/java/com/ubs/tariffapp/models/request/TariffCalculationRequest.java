package com.ubs.tariffapp.models.request;

public class TariffCalculationRequest {
    private String reporterCode;
    private String partnerCode;
    private String productCode;
    private double amountOfProduct;
    private String currency;

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
    public String getProductCode() {
        return productCode;
    }
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
    public double getAmountOfProduct() {
        return amountOfProduct;
    }
    public void setAmountOfProduct(double amountOfProduct) {
        this.amountOfProduct = amountOfProduct;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
