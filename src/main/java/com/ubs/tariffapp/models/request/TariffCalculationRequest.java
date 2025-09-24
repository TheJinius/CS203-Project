package com.ubs.tariffapp.models.request;

public class TariffCalculationRequest {
    private Integer tariffId; // Use specific tariff ID
    private double amountOfProduct;
    private String currency;

    // Constructors
    public TariffCalculationRequest() {}

    public TariffCalculationRequest(Integer tariffId, double amountOfProduct, String currency) {
        this.tariffId = tariffId;
        this.amountOfProduct = amountOfProduct;
        this.currency = currency;
    }

    // Getters and Setters
    public Integer getTariffId() { return tariffId; }
    public void setTariffId(Integer tariffId) { this.tariffId = tariffId; }

    public double getAmountOfProduct() { return amountOfProduct; }
    public void setAmountOfProduct(double amountOfProduct) { this.amountOfProduct = amountOfProduct; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
