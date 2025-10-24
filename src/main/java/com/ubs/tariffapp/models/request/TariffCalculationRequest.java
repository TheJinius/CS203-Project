package com.ubs.tariffapp.models.request;

public class TariffCalculationRequest {
    private String reporterCode;
    private String partnerCode;
    private String productCode;
    private Integer tariffId;
    private Double amountOfProduct;
    private String currency;
    private Double productValueDollars; // For Combined Duty: dollar value for Ad Valorem component

    // Constructors
    public TariffCalculationRequest() {}

    public TariffCalculationRequest(String reporterCode, String partnerCode, String productCode, 
                                    Integer tariffId, Double amountOfProduct, String currency) {
        this.reporterCode = reporterCode;
        this.partnerCode = partnerCode;
        this.productCode = productCode;
        this.tariffId = tariffId;
        this.amountOfProduct = amountOfProduct;
        this.currency = currency;
    }

    // Getters and Setters
    public String getReporterCode() { return reporterCode; }
    public void setReporterCode(String reporterCode) { this.reporterCode = reporterCode; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public Integer getTariffId() { return tariffId; }
    public void setTariffId(Integer tariffId) { this.tariffId = tariffId; }

    public Double getAmountOfProduct() { return amountOfProduct; }
    public void setAmountOfProduct(Double amountOfProduct) { this.amountOfProduct = amountOfProduct; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getProductValueDollars() { return productValueDollars; }
    public void setProductValueDollars(Double productValueDollars) { this.productValueDollars = productValueDollars; }
}
